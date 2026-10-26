package com.leftoverchef.app.data.db

import androidx.room.*
import com.leftoverchef.app.data.model.PasswordResetCode

@Dao
interface PasswordResetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCode(code: PasswordResetCode)

    @Query("SELECT * FROM password_reset_codes WHERE user_id = :userId ORDER BY id DESC LIMIT 1")
    suspend fun getLatestCode(userId: Int): PasswordResetCode?

    @Query("DELETE FROM password_reset_codes WHERE user_id = :userId")
    suspend fun deleteCodesForUser(userId: Int)

    @Query("DELETE FROM password_reset_codes WHERE expiry_millis < :nowMillis")
    suspend fun deleteExpiredCodes(nowMillis: Long)
}
