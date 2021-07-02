package com.benben.deadlockmonitordemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.benben.deadlockmonitordemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val deadLockCheck = DeadLockCheck()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.createDeadLock.setOnClickListener {
            DeadLockUtil.createDeadLock()
        }
        binding.init.setOnClickListener {
            deadLockCheck.init()
        }
        binding.checkDeadLock.setOnClickListener {
            deadLockCheck.startCheckDeadLock()
        }
        binding.release.setOnClickListener {
            deadLockCheck.release()
        }
        binding.isInit.setOnClickListener {
            deadLockCheck.isInit()
        }
    }
}