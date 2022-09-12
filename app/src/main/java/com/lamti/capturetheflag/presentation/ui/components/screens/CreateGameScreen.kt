package com.lamti.capturetheflag.presentation.ui.components.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.BattleMiniGame
import com.lamti.capturetheflag.presentation.ui.components.composables.common.DefaultButton
import com.lamti.capturetheflag.presentation.ui.components.composables.common.InfoTextField
import com.lamti.capturetheflag.presentation.ui.style.Blue
import com.lamti.capturetheflag.presentation.ui.style.White
import com.lamti.capturetheflag.utils.EMPTY
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior

@Composable
fun CreateGameScreen(onCreateGameClicked: (String, BattleMiniGame) -> Unit) {
    var gameTitle by rememberSaveable { mutableStateOf(EMPTY) }
    var miniGame by rememberSaveable { mutableStateOf(BattleMiniGame.None) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.create_game),
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Bold)
        )
        InfoTextField(
            modifier = Modifier.padding(top = 20.dp),
            text = gameTitle,
            label = stringResource(id = R.string.type_title),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words
            )
        ) {
            gameTitle = it
        }
        Text(
            modifier = Modifier.padding(top = 20.dp),
            text = stringResource(R.string.choose_mini_game),
            style = MaterialTheme.typography.h6
        )
        MiniGamesList(modifier = Modifier.padding(top = 10.dp)) {
            miniGame = it
        }
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .weight(1f),
            painter = painterResource(id = R.drawable.create_game),
            contentDescription = "create game image"
        )
        DefaultButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            text = stringResource(id = R.string.create),
        ) {
            if (gameTitle.isEmpty()) return@DefaultButton
            onCreateGameClicked(gameTitle, miniGame)
        }
    }
}

data class MiniGame(
    val value: BattleMiniGame,
    val id: Int,
)

@Composable
private fun MiniGamesList(modifier: Modifier = Modifier, onItemClicked: (BattleMiniGame) -> Unit) {
    val lazyListState: LazyListState = rememberLazyListState()
    var selectedIndex by remember { mutableStateOf(0) }
    val list = remember { BattleMiniGame.values().map { MiniGame(it, it.ordinal) }.toMutableList() }

    LazyRow(modifier = modifier, flingBehavior = rememberSnapperFlingBehavior(lazyListState)) {
        items(list) {
            MiniGameItem(miniGame = it, selectedIndex = selectedIndex, onItemClicked = { miniGame ->
                selectedIndex = if (selectedIndex != miniGame.id) miniGame.id else -1
                onItemClicked(miniGame.value)
            })
        }
    }
}

@Composable
private fun MiniGameItem(
    modifier: Modifier = Modifier,
    miniGame: MiniGame,
    selectedIndex: Int,
    shape: Shape = RoundedCornerShape(10.dp),
    selectedColor: Color = Blue,
    onItemClicked: (MiniGame) -> Unit
) {
    Column(
        modifier = modifier
            .selectable(
                selected = miniGame.id == selectedIndex,
                onClick = { onItemClicked(miniGame) }
            )
            .clip(shape)
            .background(if (miniGame.id == selectedIndex) selectedColor else Color.Transparent)
            .padding(8.dp)
    ) {
        Text(
            text = miniGame.value.name,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (miniGame.id == selectedIndex) White else selectedColor
        )
    }
}


