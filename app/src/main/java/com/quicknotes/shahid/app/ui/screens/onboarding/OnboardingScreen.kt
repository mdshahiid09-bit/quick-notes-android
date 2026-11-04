package com.quicknotes.shahid.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.shahid.app.R
import com.quicknotes.shahid.app.data.local.preferences.UserPreferencesRepository
import com.quicknotes.shahid.app.ui.navigation.Screen
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val titleRes: Int,
    val descRes: Int,
    val iconRes: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    preferencesRepository: UserPreferencesRepository,
    onNavigate: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val pages = listOf(
        OnboardingPageData(
            titleRes = R.string.onboarding_slide1_title,
            descRes = R.string.onboarding_slide1_desc,
            iconRes = R.drawable.ic_empty_notes
        ),
        OnboardingPageData(
            titleRes = R.string.onboarding_slide2_title,
            descRes = R.string.onboarding_slide2_desc,
            iconRes = R.drawable.ic_empty_archive
        ),
        OnboardingPageData(
            titleRes = R.string.onboarding_slide3_title,
            descRes = R.string.onboarding_slide3_desc,
            iconRes = R.drawable.ic_empty_vault
        ),
        OnboardingPageData(
            titleRes = R.string.onboarding_slide4_title,
            descRes = R.string.onboarding_slide4_desc,
            iconRes = R.drawable.ic_app_logo
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    fun finishOnboarding() {
        coroutineScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
            onNavigate(Screen.VaultSetup.route)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            AnimatedVisibility(visible = pagerState.currentPage < pages.size - 1) {
                TextButton(onClick = { finishOnboarding() }) {
                    Text(
                        text = stringResource(R.string.action_skip),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.2f))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterVertically,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = page.iconRes),
                    contentDescription = stringResource(page.titleRes),
                    modifier = Modifier.size(140.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = stringResource(page.titleRes),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(page.descRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 28.dp else 10.dp,
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .height(10.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        finishOnboarding()
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.size - 1) {
                        stringResource(R.string.action_get_started)
                    } else {
                        stringResource(R.string.action_next)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
