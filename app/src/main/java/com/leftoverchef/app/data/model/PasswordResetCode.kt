package com.leftoverchef.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "password_reset_codes")
data class PasswordResetCode(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id")      val userId: Int,
    @ColumnInfo(name = "code_hash")    val codeHash: String,
    @ColumnInfo(name = "expiry_millis") val expiryMillis: Long
)
