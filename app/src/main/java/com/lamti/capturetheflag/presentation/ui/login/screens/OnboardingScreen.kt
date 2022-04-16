package com.lamti.capturetheflag.presentation.ui.login.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.ui.components.composables.common.DefaultButton
import com.lamti.capturetheflag.presentation.ui.components.composables.common.PermissionsCard
import com.lamti.capturetheflag.presentation.ui.style.Blue
import com.lamti.capturetheflag.presentation.ui.style.Green
import com.lamti.capturetheflag.presentation.ui.style.Red
import com.lamti.capturetheflag.presentation.ui.style.White
import com.lamti.capturetheflag.presentation.ui.style.WhiteOpacity

data class Page(
    val color: Color,
    val title: String,
    val subtitle: String? = null,
    val description: String,
    @DrawableRes val image: Int
)

@Composable
fun OnboardingScreen(
    hasPermissions: Boolean,
    next: Int,
    onStartButtonClicked: () -> Unit,
    onPermissionsOkClicked: () -> Unit
) {
    val pages = listOf(
        Page(
            color = Red,
            title = stringResource(id = R.string.capture_the_flag),
            description = stringResource(R.string.onboarding1),
            image = R.drawable.onboarding1
        ),
        Page(
            color = Green,
            title = stringResource(R.string.fight_battles),
            description = stringResource(R.string.onboarding2),
            image = R.drawable.onboarding2
        ),
        Page(
            color = Blue,
            title = stringResource(R.string.capture_the_flag),
            subtitle = stringResource(id = R.string.location_permissions),
            description = stringResource(R.string.location_permissions_required),
            image = R.drawable.intro_logo
        ),
        Page(
            color = Blue,
            title = stringResource(R.string.assemble_team),
            description = stringResource(R.string.onboarding3),
            image = R.drawable.green_battle_image
        ),
    )
    val pagerState = rememberPagerState(pageCount = pages.size)
    val imagePadding = animateFloatAsState(if (pagerState.currentPage == 3) 100f else 0f)
    val indicatorOffset = animateFloatAsState(if (pagerState.currentPage == 2) 70f else 0f)

    LaunchedEffect(key1 = next) {
        if(hasPermissions) pagerState.animateScrollToPage(3)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
            verticalAlignment = Alignment.Top
        ) { position ->
            val page = pages[position]
            if (page.subtitle == null)
                PagerScreen(page = pages[position], imagePadding = imagePadding.value.dp)
            else
                PermissionPagerScreen(
                    page = pages[position],
                    hasPermissions = hasPermissions,
                    onOkClicked = onPermissionsOkClicked
                )
        }
        HorizontalPagerIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = indicatorOffset.value.dp),
            activeColor = White,
            inactiveColor = WhiteOpacity,
            pagerState = pagerState
        )
        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            visible = pagerState.currentPage == 3
        ) {
            DefaultButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                text = stringResource(id = R.string.start),
                cornerSize = CornerSize(20.dp),
                color = White,
                textColor = Blue,
                onclick = onStartButtonClicked
            )
        }
    }
}

@Composable
fun PagerScreen(page: Page, imagePadding: Dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(page.color),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = page.title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h4.copy(
                color = White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

        )
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            text = page.description,
            style = MaterialTheme.typography.subtitle1.copy(color = White, textAlign = TextAlign.Center)
        )
        Spacer(modifier = Modifier.height(160.dp))
        Image(
            modifier = Modifier
                .height(330.dp)
                .padding(bottom = imagePadding),
            painter = painterResource(id = page.image),
            contentDescription = "Pager Image"
        )
    }
}

@Composable
fun PermissionPagerScreen(
    page: Page,
    hasPermissions: Boolean,
    onOkClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(page.color),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = page.title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h4.copy(
                color = White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

        )
        Image(
            modifier = Modifier
                .height(330.dp)
                .padding(),
            painter = painterResource(id = page.image),
            contentDescription = "Pager Image"
        )
        PermissionsCard(
            title = page.subtitle!!,
            description = page.description,
            hasPermissions = hasPermissions,
            onOkClicked = onOkClicked
        )
    }
}
