package com.leftoverchef.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.leftoverchef.app.data.model.Ingredient
import com.leftoverchef.app.data.model.SavedRecipe
import com.leftoverchef.app.data.model.CalorieLogEntry
import com.leftoverchef.app.data.model.User
import com.leftoverchef.app.data.model.PasswordResetCode

@Database(
    entities = [Ingredient::class, SavedRecipe::class, CalorieLogEntry::class, User::class,
                PasswordResetCode::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ingredientDao(): IngredientDao
    abstract fun savedRecipeDao(): SavedRecipeDao
    abstract fun calorieLogDao(): CalorieLogDao
    abstract fun userDao(): UserDao
    abstract fun passwordResetDao(): PasswordResetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migracije cuvaju stare korisnicke podatke kada se struktura baze promeni
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE ingredients ADD COLUMN category TEXT NOT NULL DEFAULT 'Other'"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS calorie_log (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recipeId INTEGER NOT NULL,
                        recipeTitle TEXT NOT NULL,
                        calories REAL NOT NULL,
                        protein REAL NOT NULL,
                        carbs REAL NOT NULL,
                        fat REAL NOT NULL,
                        servingsEaten INTEGER NOT NULL,
                        dateMillis INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        username TEXT NOT NULL,
                        email TEXT NOT NULL,
                        password_hash TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """)
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_users_email ON users (email)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE ingredients ADD COLUMN userId INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE calorie_log ADD COLUMN userId INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS saved_recipes_new (
                        id INTEGER NOT NULL,
                        userId INTEGER NOT NULL DEFAULT 0,
                        title TEXT NOT NULL,
                        image TEXT NOT NULL,
                        calories REAL NOT NULL DEFAULT 0.0,
                        protein REAL NOT NULL DEFAULT 0.0,
                        carbs REAL NOT NULL DEFAULT 0.0,
                        fat REAL NOT NULL DEFAULT 0.0,
                        readyInMinutes INTEGER NOT NULL DEFAULT 0,
                        servings INTEGER NOT NULL DEFAULT 1,
                        sourceUrl TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(id, userId)
                    )
                """)
                database.execSQL("""
                    INSERT INTO saved_recipes_new
                        (id, userId, title, image, calories, protein, carbs, fat, readyInMinutes, servings, sourceUrl)
                    SELECT id, 0, title, image, calories, protein, carbs, fat, readyInMinutes, servings, sourceUrl
                    FROM saved_recipes
                """)
                database.execSQL("DROP TABLE saved_recipes")
                database.execSQL("ALTER TABLE saved_recipes_new RENAME TO saved_recipes")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS password_reset_codes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        user_id INTEGER NOT NULL,
                        code_hash TEXT NOT NULL,
                        expiry_millis INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Room baza je singleton da cijela aplikacija koristi jednu instancu.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "leftover_chef_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
