package com.example.smarthomesystem

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object FirebaseRepository {
    private val database = FirebaseDatabase.getInstance()

    // ---------- FLOORS ----------
    fun listenToFloors(onDataChanged: (List<Floor>) -> Unit) {
        val floorsRef = database.getReference("floors")
        floorsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val floors = mutableListOf<Floor>()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: id
                    floors.add(Floor(id = id, name = name))
                }
                onDataChanged(floors)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ---------- DEVICES ----------
    fun listenToDevices(
        floorId: String,
        onDataChanged: (List<Device>) -> Unit
    ) {
        val devicesRef = database.getReference("floors/$floorId/devices")

        devicesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val devices = mutableListOf<Device>()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val type = child.child("type").getValue(String::class.java) ?: ""
                    val name = child.child("name").getValue(String::class.java) ?: ""
                    val state = child.child("state").getValue(String::class.java) ?: "OFF"
                    val maxDurationSec = child.child("maxDurationSec").getValue(Int::class.java) ?: 0
                    val turnedOnAt = child.child("turnedOnAt").getValue(Long::class.java) ?: 0L
                    val streamUrl = child.child("streamUrl").getValue(String::class.java) ?: ""
                    val scheduleEnabled = child.child("scheduleEnabled").getValue(Boolean::class.java) ?: false
                    val scheduleStartHour = child.child("scheduleStartHour").getValue(Int::class.java) ?: 0
                    val scheduleEndHour = child.child("scheduleEndHour").getValue(Int::class.java) ?: 0

                    val switchesSnapshot = child.child("switches")
                    val switches = mutableMapOf<String, String>()
                    for (sw in switchesSnapshot.children) {
                        val swId = sw.key ?: continue
                        val swState = sw.getValue(String::class.java) ?: "OFF"
                        switches[swId] = swState
                    }

                    devices.add(
                        Device(
                            id = id, name = name, type = type, state = state,
                            switches = switches, maxDurationSec = maxDurationSec,
                            turnedOnAt = turnedOnAt, streamUrl = streamUrl,
                            scheduleEnabled = scheduleEnabled,
                            scheduleStartHour = scheduleStartHour,
                            scheduleEndHour = scheduleEndHour
                        )
                    )
                }
                onDataChanged(devices)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
    fun updateDeviceState(floorId: String, deviceId: String, newState: String, turnedOnAt: Long = 0L) {
        val deviceRef = database.getReference("floors/$floorId/devices/$deviceId")
        deviceRef.child("state").setValue(newState)
        if (newState == "ON") {
            deviceRef.child("turnedOnAt").setValue(turnedOnAt)
        }
    }

    fun updateSwitchState(floorId: String, deviceId: String, switchId: String, newState: String) {
        database.getReference("floors/$floorId/devices/$deviceId/switches/$switchId").setValue(newState)
    }

    // ---------- USAGE LOGS ----------
    fun logUsage(deviceId: String, deviceName: String, action: String) {
        val logsRef = database.getReference("usageLogs").push()
        val log = mapOf(
            "deviceId" to deviceId,
            "deviceName" to deviceName,
            "action" to action,
            "timestamp" to System.currentTimeMillis()
        )
        logsRef.setValue(log)
    }

    fun listenToLogs(onDataChanged: (List<UsageLog>) -> Unit) {
        val logsRef = database.getReference("usageLogs")
        logsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val logs = mutableListOf<UsageLog>()
                for (child in snapshot.children) {
                    val deviceId = child.child("deviceId").getValue(String::class.java) ?: ""
                    val deviceName = child.child("deviceName").getValue(String::class.java) ?: ""
                    val action = child.child("action").getValue(String::class.java) ?: ""
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    logs.add(UsageLog(deviceId, deviceName, action, timestamp))
                }
                onDataChanged(logs)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}