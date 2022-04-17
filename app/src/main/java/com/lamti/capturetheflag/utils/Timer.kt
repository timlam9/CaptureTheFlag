package com.lamti.capturetheflag.utils

import android.os.CountDownTimer
import timber.log.Timber


fun startTimer(
    timeInMillis: Long,
    onTick: (Long) -> Unit,
    onFinish: () -> Unit
) =
    object : CountDownTimer(timeInMillis, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            Timber.d("Timer: $millisUntilFinished")
            onTick(millisUntilFinished)
        }

        override fun onFinish() {
            Timber.d("Timer finished")
            onFinish()
        }
    }.apply {
        start()
    }

