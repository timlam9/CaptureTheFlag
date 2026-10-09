package com.lamti.capturetheflag.domain.game

import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.data.firestore.BattleRaw
import com.lamti.capturetheflag.data.firestore.BattleRaw.Companion.toRaw
import com.lamti.capturetheflag.domain.player.Team
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class BattleSynchronizationTest {

    private fun game(miniGame: BattleMiniGame = BattleMiniGame.TapTheFlag): Game {
        val fence = GeofenceObject(LatLng(0.0, 0.0), false, false, "", Date(0))
        return Game(
            gameID = "game", title = "", flagRadius = 1f, gameRadius = 1f,
            gameState = GameState(fence, fence, fence, "red1", "green1", ProgressState.Started, Team.Unknown),
            battleMiniGame = miniGame,
            redPlayers = listOf(ActivePlayer("red1", false), ActivePlayer("red2", false)),
            greenPlayers = listOf(ActivePlayer("green1", false), ActivePlayer("green2", false)),
            battles = emptyList()
        )
    }

    private fun battle(
        id: String = "battle", red: String = "red1", green: String = "green1",
        state: BattleState = BattleState.StandBy
    ) = Battle(id, state, "", listOf(BattlingPlayer(red, false), BattlingPlayer(green, false)))

    private fun withBattle(battle: Battle = battle(), miniGame: BattleMiniGame = BattleMiniGame.TapTheFlag) =
        game(miniGame).copy(battles = listOf(battle))

    @Test fun addIsPureAndReciprocalChallengesAreIdempotent() {
        val original = game()
        val challenge = battle()
        val added = BattleSynchronization.add(original, challenge)!!
        assertTrue(original.battles.isEmpty())
        assertEquals(listOf(challenge), added.battles)
        assertSame(added, BattleSynchronization.add(added, challenge))
        assertSame(added, BattleSynchronization.add(added, challenge.copy(
            battleID = "reciprocal", players = challenge.players.reversed()
        )))
        val started = added.copy(battles = listOf(challenge.copy(state = BattleState.Started)))
        assertSame(started, BattleSynchronization.add(started, challenge))
    }

    @Test fun disjointChallengesAreAllowedButSharedParticipantsAreReserved() {
        val existing = withBattle()
        val disjoint = battle("other", "red2", "green2")
        assertEquals(2, BattleSynchronization.add(existing, disjoint)!!.battles.size)
        assertNull(BattleSynchronization.add(existing, battle("other", "red2", "green1")))
        assertNull(BattleSynchronization.add(existing, battle("other", "red1", "green2")))
        assertNull(BattleSynchronization.add(existing, disjoint.copy(battleID = "battle")))
        val over = withBattle(battle().copy(state = BattleState.Over, winner = "name", winnerID = "red1"))
        assertNull(BattleSynchronization.add(over, battle("reciprocal")))
        assertNull(BattleSynchronization.add(over, battle("other", "red2", "green1")))
        val partiallyAcknowledged = over.copy(battles = listOf(over.battles.single().copy(
            players = listOf(BattlingPlayer("green1", true))
        )))
        assertNull(BattleSynchronization.add(partiallyAcknowledged, battle("other", "red2", "green1")))
    }

    @Test fun addRequiresStartedGameAndEligibleOpposingRosterPlayers() {
        val original = game()
        ProgressState.values().filter { it != ProgressState.Started }.forEach {
            assertNull(BattleSynchronization.add(original.copy(gameState = original.gameState.copy(state = it)), battle()))
        }
        assertNull(BattleSynchronization.add(original, battle(green = "red2")))
        assertNull(BattleSynchronization.add(original, battle(red = "green2")))
        assertNull(BattleSynchronization.add(original, battle(red = "unknown")))
        assertNull(BattleSynchronization.add(original.copy(redPlayers = listOf(ActivePlayer("red1", true))), battle()))
        assertNull(BattleSynchronization.add(original.copy(greenPlayers = listOf(ActivePlayer("green1", true))), battle()))
        assertNull(BattleSynchronization.add(original.copy(greenPlayers = original.greenPlayers + ActivePlayer("red1", false)), battle()))
    }

    @Test fun addRejectsMalformedChallenges() {
        val challenge = battle()
        val malformed = listOf(
            challenge.copy(battleID = " "),
            challenge.copy(state = BattleState.Started),
            challenge.copy(state = BattleState.Over),
            challenge.copy(winner = "name"),
            challenge.copy(winner = " "),
            challenge.copy(winnerID = "red1"),
            challenge.copy(players = emptyList()),
            challenge.copy(players = challenge.players.take(1)),
            challenge.copy(players = challenge.players + BattlingPlayer("green2", false)),
            challenge.copy(players = listOf(BattlingPlayer("red1", false), BattlingPlayer("red1", false))),
            challenge.copy(players = listOf(BattlingPlayer(" ", false), BattlingPlayer("green1", false))),
            challenge.copy(players = challenge.players.map { it.copy(ready = true) })
        )
        malformed.forEach { assertNull(it.toString(), BattleSynchronization.add(game(), it)) }
    }

    @Test fun readinessStartsOnlyAfterBothPlayersAreReady() {
        val original = withBattle()
        val first = BattleSynchronization.ready(original, "battle", "red1")!!
        assertEquals(BattleState.StandBy, first.battles.single().state)
        assertEquals(listOf(true, false), first.battles.single().players.map { it.ready })
        assertEquals(first, BattleSynchronization.ready(first, "battle", "red1"))
        val started = BattleSynchronization.ready(first, "battle", "green1")!!
        assertEquals(BattleState.Started, started.battles.single().state)
        assertTrue(started.battles.single().players.all { it.ready })
        assertSame(started, BattleSynchronization.ready(started, "battle", "red1"))
        assertTrue(original.battles.single().players.none { it.ready })
    }

    @Test fun readinessRejectsStaleRequestsAndDoesNotTouchOtherBattles() {
        val original = withBattle().copy(battles = listOf(battle(), battle("other", "red2", "green2")))
        assertNull(BattleSynchronization.ready(original, "missing", "red1"))
        assertNull(BattleSynchronization.ready(original, "battle", "red2"))
        assertNull(BattleSynchronization.ready(withBattle(battle(state = BattleState.Over)), "battle", "red1"))
        val updated = BattleSynchronization.ready(original, "battle", "green1")!!
        assertEquals(original.battles[1], updated.battles[1])
        val onePlayer = withBattle(battle().copy(players = listOf(BattlingPlayer("red1", false))))
        assertEquals(BattleState.StandBy, BattleSynchronization.ready(onePlayer, "battle", "red1")!!.battles.single().state)
        val threePlayers = withBattle(battle().copy(players = listOf(
            BattlingPlayer("red1", false), BattlingPlayer("green1", true), BattlingPlayer("green2", true)
        )))
        assertEquals(BattleState.StandBy, BattleSynchronization.ready(threePlayers, "battle", "red1")!!.battles.single().state)
    }

    @Test fun finishIsFirstClaimOnlyAndStoresWinnerIdentity() {
        val original = withBattle(battle(state = BattleState.Started))
        val finished = BattleSynchronization.finish(original, "battle", "red1", "winner")!!
        assertEquals(BattleState.Over, finished.battles.single().state)
        assertEquals("winner", finished.battles.single().winner)
        assertEquals("red1", finished.battles.single().winnerID)
        assertSame(finished, BattleSynchronization.finish(finished, "battle", "green1", "other"))
        assertSame(finished, BattleSynchronization.finish(finished, "battle", "red1", ""))
        assertEquals(BattleState.Started, original.battles.single().state)
    }

    @Test fun finishRejectsInvalidClaims() {
        val started = withBattle(battle(state = BattleState.Started))
        assertNull(BattleSynchronization.finish(started, "missing", "red1", "name"))
        assertNull(BattleSynchronization.finish(started, "battle", "outsider", "name"))
        assertNull(BattleSynchronization.finish(started, "battle", "red1", " \n"))
        assertNull(BattleSynchronization.finish(withBattle(), "battle", "red1", "name"))
        val over = withBattle(battle(state = BattleState.Over))
        assertNull(BattleSynchronization.finish(over, "battle", "outsider", "name"))
    }

    @Test fun minigameAcknowledgmentsWorkInBothOrdersEvenWithIdenticalUsernames() {
        listOf(listOf("red1", "green1"), listOf("green1", "red1")).forEach { order ->
            val finished = BattleSynchronization.finish(withBattle(battle(state = BattleState.Started)), "battle", "red1", "same")!!
            val first = BattleSynchronization.acknowledge(finished, "battle", order[0], "same")!!
            assertEquals(listOf(order[1]), first.battles.single().players.map { it.id })
            assertSame(first, BattleSynchronization.acknowledge(first, "battle", order[0], "same"))
            assertNull(BattleSynchronization.add(first, battle("other", "red2", "green1")))
            val final = BattleSynchronization.acknowledge(first, "battle", order[1], "same")!!
            assertTrue(final.battles.isEmpty())
            assertFalse(final.redPlayers.first().hasLost)
            assertTrue(final.greenPlayers.first().hasLost)
            assertFalse(final.redPlayers[1].hasLost)
            assertFalse(final.greenPlayers[1].hasLost)
            assertEquals("red1", final.gameState.greenFlagCaptured)
            assertNull(final.gameState.redFlagCaptured)
            assertSame(final, BattleSynchronization.acknowledge(final, "battle", "green1", "same"))
        }
    }

    @Test fun winnerAcknowledgmentNeverClearsFlagsOrResetsLossState() {
        val original = withBattle(battle(state = BattleState.Over).copy(winner = "name", winnerID = "red1"))
        val winnerAlreadyLost = original.copy(redPlayers = listOf(ActivePlayer("red1", true)))
        val updated = BattleSynchronization.acknowledge(winnerAlreadyLost, "battle", "red1", "different")!!
        assertEquals(winnerAlreadyLost.gameState, updated.gameState)
        assertEquals(winnerAlreadyLost.redPlayers, updated.redPlayers)
        assertEquals(winnerAlreadyLost.greenPlayers, updated.greenPlayers)
    }

    @Test fun acknowledgmentsAreScopedAndRequireFinishedMinigameOutcome() {
        val original = withBattle()
        assertNull(BattleSynchronization.acknowledge(original, "battle", "red1", "name"))
        assertNull(BattleSynchronization.acknowledge(withBattle(battle(state = BattleState.Started)), "battle", "red1", "name"))
        assertNull(BattleSynchronization.acknowledge(withBattle(battle(state = BattleState.Over)), "battle", "red1", "name"))
        assertSame(original, BattleSynchronization.acknowledge(original, "missing", "red1", "name"))
        assertSame(original, BattleSynchronization.acknowledge(original, "battle", "outsider", "name"))
        val finished = battle(state = BattleState.Over).copy(winner = "name", winnerID = "red1")
        val multiple = game().copy(battles = listOf(finished, battle("other", "red2", "green2")))
        val updated = BattleSynchronization.acknowledge(multiple, "battle", "green1", "loser")!!
        assertEquals(multiple.battles[1], updated.battles[1])
        assertEquals(2, multiple.battles.first().players.size)
    }

    @Test fun legacyNameOnlyOutcomesStillWork() {
        val original = withBattle(battle(state = BattleState.Over).copy(winner = "winner"))
        val winner = BattleSynchronization.acknowledge(original, "battle", "red1", "winner")!!
        assertFalse(winner.redPlayers.first().hasLost)
        assertEquals(original.gameState, winner.gameState)
        val loser = BattleSynchronization.acknowledge(winner, "battle", "green1", "loser")!!
        assertTrue(loser.greenPlayers.first().hasLost)
    }

    @Test fun concessionReservesOpponentAndWinnerAcknowledgmentIsSafe() {
        val original = withBattle(miniGame = BattleMiniGame.None)
        val conceded = BattleSynchronization.acknowledge(original, "battle", "red1", "same")!!
        val remaining = conceded.battles.single()
        assertEquals(BattleState.Over, remaining.state)
        assertEquals("green1", remaining.winnerID)
        assertEquals(listOf("green1"), remaining.players.map { it.id })
        assertTrue(conceded.redPlayers.first().hasLost)
        assertFalse(conceded.greenPlayers.first().hasLost)
        assertNull(conceded.gameState.greenFlagCaptured)
        assertEquals("green1", conceded.gameState.redFlagCaptured)
        assertSame(conceded, BattleSynchronization.acknowledge(conceded, "battle", "red1", "same"))
        assertNull(BattleSynchronization.add(conceded, battle("other", "red2", "green1")))
        val acknowledged = BattleSynchronization.acknowledge(conceded, "battle", "green1", "same")!!
        assertTrue(acknowledged.battles.isEmpty())
        assertEquals(conceded.gameState, acknowledged.gameState)
        assertFalse(acknowledged.greenPlayers.first().hasLost)
        assertNotNull(BattleSynchronization.add(acknowledged, battle("other", "red2", "green1")))
    }

    @Test fun noneOverAcknowledgmentsWorkInBothOrdersWithoutDisplayWinner() {
        listOf(listOf("red1", "green1"), listOf("green1", "red1")).forEach { order ->
            val original = withBattle(battle(state = BattleState.Over).copy(winnerID = "green1"), BattleMiniGame.None)
            val first = BattleSynchronization.acknowledge(original, "battle", order[0], "same")!!
            val final = BattleSynchronization.acknowledge(first, "battle", order[1], "same")!!
            assertTrue(final.battles.isEmpty())
            assertTrue(final.redPlayers.first().hasLost)
            assertFalse(final.greenPlayers.first().hasLost)
            assertNull(final.gameState.greenFlagCaptured)
            assertEquals("green1", final.gameState.redFlagCaptured)
        }
        val legacy = withBattle(battle(state = BattleState.Over).copy(winner = "green1"), BattleMiniGame.None)
        assertFalse(BattleSynchronization.acknowledge(legacy, "battle", "green1", "name")!!.greenPlayers.first().hasLost)
    }

    @Test fun concessionRejectsStartedAndMalformedOpponent() {
        assertNull(BattleSynchronization.acknowledge(withBattle(battle(state = BattleState.Started), BattleMiniGame.None), "battle", "red1", "name"))
        val alone = battle().copy(players = listOf(BattlingPlayer("red1", false)))
        assertNull(BattleSynchronization.acknowledge(withBattle(alone, BattleMiniGame.None), "battle", "red1", "name"))
    }

    @Test fun loserDropsBothFlagsOnlyWhenCarryingThem() {
        val original = withBattle(battle(state = BattleState.Over).copy(winner = "name", winnerID = "green1"))
        val bothFlags = original.copy(gameState = original.gameState.copy(redFlagCaptured = "red1", greenFlagCaptured = "red1"))
        val updated = BattleSynchronization.acknowledge(bothFlags, "battle", "red1", "loser")!!
        assertNull(updated.gameState.redFlagCaptured)
        assertNull(updated.gameState.greenFlagCaptured)
    }

    @Test fun departureReleasesOpponentAndPreservesUnrelatedBattles() {
        val original = withBattle().copy(battles = listOf(battle(), battle("other", "red2", "green2")))
        val departed = BattleSynchronization.leave(original, "red1")
        assertEquals(listOf("red2"), departed.redPlayers.map { it.id })
        assertEquals(listOf(original.battles[1]), departed.battles)
        assertFalse(departed.greenPlayers.first().hasLost)
        assertNull(departed.gameState.greenFlagCaptured)
        assertEquals(original.gameState.redFlagCaptured, departed.gameState.redFlagCaptured)
        assertEquals(departed, BattleSynchronization.leave(departed, "red1"))
    }

    private fun multiplayerLobby(): Game = BattleSynchronization.add(
        game().copy(multiplayerBattles = true), battle(), 0
    )!!

    @Test fun multiplayerReadyStartsTenSecondCountdownAndDuplicateReadyDoesNotResetIt() {
        val first = BattleSynchronization.ready(multiplayerLobby(), "battle", "red1", 1_000)!!
        assertEquals(11_000L, first.battles.single().countdownEndsAt)
        assertEquals(BattleState.StandBy, first.battles.single().state)
        assertEquals(first, BattleSynchronization.ready(first, "battle", "red1", 2_000))
        val second = BattleSynchronization.ready(first, "battle", "green1", 3_000)!!
        assertEquals(13_000L, second.battles.single().countdownEndsAt)
        assertEquals(BattleState.StandBy, second.battles.single().state)
    }

    @Test fun newArrivalResetsCountdownAndStaleStartCannotStartEarly() {
        val ready = BattleSynchronization.ready(multiplayerLobby(), "battle", "red1", 1_000)!!
        val joined = BattleSynchronization.join(ready, "battle", "red2", 5_000)!!
        assertEquals(3, joined.battles.single().players.size)
        assertEquals(15_000L, joined.battles.single().countdownEndsAt)
        assertEquals(joined, BattleSynchronization.join(joined, "battle", "red2", 6_000))
        assertNull(BattleSynchronization.start(joined, "battle", "red1", 11_000))
        val started = BattleSynchronization.start(joined, "battle", "red1", 15_000)!!
        assertEquals(BattleState.Started, started.battles.single().state)
        assertNull(BattleSynchronization.join(started, "battle", "green2", 15_000))
    }

    @Test fun deadlineLocksLobbyEvenBeforeStartWriteAndLateReadyDoesNotResetIt() {
        val ready = BattleSynchronization.ready(multiplayerLobby(), "battle", "red1", 1_000)!!
        assertNull(BattleSynchronization.join(ready, "battle", "red2", 11_000))
        assertNull(BattleSynchronization.add(ready, battle("other", "red2", "green1"), 11_000))
        val lateReady = BattleSynchronization.ready(ready, "battle", "green1", 11_000)!!
        assertEquals(BattleState.Started, lateReady.battles.single().state)
        assertEquals(11_000L, lateReady.battles.single().countdownEndsAt)
        assertNull(BattleSynchronization.start(ready, "battle", "outsider", 11_000))
    }

    @Test fun joiningBeforeReadyDoesNotStartCountdownAndReservationsStillApply() {
        val lobby = multiplayerLobby()
        val joined = BattleSynchronization.join(lobby, "battle", "red2", 1_000)!!
        assertNull(joined.battles.single().countdownEndsAt)
        assertNull(BattleSynchronization.start(joined, "battle", "red1", 20_000))
        assertNull(BattleSynchronization.join(lobby, "battle", "unknown", 1_000))
        val busy = lobby.copy(battles = lobby.battles + battle("other", "red2", "green2"))
        assertNull(BattleSynchronization.join(busy, "battle", "red2", 1_000))
        val eliminated = lobby.copy(redPlayers = lobby.redPlayers.map {
            if (it.id == "red2") it.copy(hasLost = true) else it
        })
        assertNull(BattleSynchronization.join(eliminated, "battle", "red2", 1_000))
    }

    @Test fun challengeAgainstLobbyParticipantJoinsExistingBattle() {
        val lobby = multiplayerLobby()
        val joined = BattleSynchronization.add(lobby, battle("other", "red2", "green1"), 1_000)!!
        assertEquals(1, joined.battles.size)
        assertEquals(setOf("red1", "green1", "red2"), joined.battles.single().players.map { it.id }.toSet())
    }

    @Test fun multiplayerOutcomeCanBeAcknowledgedByEveryParticipant() {
        val lobby = BattleSynchronization.join(multiplayerLobby(), "battle", "green2", 0)!!
        val ready = BattleSynchronization.ready(lobby, "battle", "red1", 1_000)!!
        val started = BattleSynchronization.start(ready, "battle", "green1", 11_000)!!
        val finished = BattleSynchronization.finish(started, "battle", "green2", "winner")!!
        var acknowledged = finished
        for (id in listOf("red1", "green1", "green2")) {
            acknowledged = BattleSynchronization.acknowledge(acknowledged, "battle", id, id)!!
        }
        assertTrue(acknowledged.battles.isEmpty())
        assertTrue(acknowledged.redPlayers.first().hasLost)
        assertTrue(acknowledged.greenPlayers.first().hasLost)
        assertFalse(acknowledged.greenPlayers[1].hasLost)
    }

    @Test fun multiplayerDepartureDoesNotCancelStartedBattle() {
        val lobby = BattleSynchronization.join(multiplayerLobby(), "battle", "green2", 0)!!
        val ready = BattleSynchronization.ready(lobby, "battle", "red1", 1_000)!!
        val started = BattleSynchronization.start(ready, "battle", "red1", 11_000)!!
        val departed = BattleSynchronization.leave(started, "red1")
        assertEquals(BattleState.Started, departed.battles.single().state)
        assertEquals(setOf("green1", "green2"), departed.battles.single().players.map { it.id }.toSet())
    }

    @Test fun multiplayerFieldsRoundTripAndLegacyDefaultsRemainDisabled() {
        val lobby = BattleSynchronization.ready(multiplayerLobby(), "battle", "red1", 1_000)!!.battles.single()
        assertEquals(lobby, lobby.toRaw().toBattle())
        assertFalse(BattleRaw().toBattle().multiplayer)
        assertNull(BattleRaw().toBattle().countdownEndsAt)
    }

    @Test fun winnerIdRoundTripsAndDefaultsForLegacyDocuments() {
        val original = battle(state = BattleState.Over).copy(winner = "same", winnerID = "red1")
        assertEquals(original, original.toRaw().toBattle())
        assertEquals("red1", original.toRaw().winnerID)
        assertEquals("", BattleRaw().winnerID)
        assertEquals("", BattleRaw(winner = "legacy").toBattle().winnerID)
    }
}
