package com.example

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.GameDatabase
import com.example.data.LeaderboardEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Maze Miner Race`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Maze Miner Race", appName)
    }

    @Test
    fun `sqlite leaderboard enforces UNIQUE fingerprint duplicate rejection`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.gameDao()
            val entry = LeaderboardEntity(
                fingerprint = "abc123deadbeef",
                seed = "2026-11-18",
                serverScore = 450,
                timeSeconds = 42,
                difficulty = "normal",
                biome = "Desert",
                season = "season-1"
            )
            val rank = dao.insertLeaderboardTransactional(entry)
            assertEquals(1, rank)
            assertTrue(dao.hasFingerprint("abc123deadbeef"))

            var duplicateRejected = false
            try {
                dao.insertLeaderboardTransactional(entry)
            } catch (_: SQLiteConstraintException) {
                duplicateRejected = true
            }
            assertTrue("SQLite PRIMARY KEY must reject duplicate replay fingerprint", duplicateRejected)
        } finally {
            db.close()
        }
    }
}
