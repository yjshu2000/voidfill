package com.upsiway.voidfill

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class TileManager(private val context: Context) {
    companion object {
        const val TILE_SIZE = 256
        private const val TILES_DIRECTORY = "tiles"
    }

    // Cache of loaded tiles
    private val tileCache = ConcurrentHashMap<Pair<Int, Int>, Tile>()

    // Set of tiles that have been modified and need to be saved
    private val modifiedTiles = mutableSetOf<Pair<Int, Int>>()

    // Get a tile at the specified coordinates, loading or creating it if necessary
    fun getTile(tileX: Int, tileY: Int): Tile? {
        val key = Pair(tileX, tileY)

        // Return from cache if available
        tileCache[key]?.let { return it }

        // Try to load from storage
        val tile = loadTile(tileX, tileY)

        // Create a new empty tile if not found
        val resultTile = tile ?: Tile(tileX, tileY)
        tileCache[key] = resultTile

        return resultTile
    }

    // Set a pixel in a specific tile
    fun setPixel(tileX: Int, tileY: Int, pixelX: Int, pixelY: Int, filled: Boolean) {
        val tile = getTile(tileX, tileY) ?: return

        // Only mark as modified if the pixel value is actually changing
        if (tile.getPixel(pixelX, pixelY) != filled) {
            tile.setPixel(pixelX, pixelY, filled)
            modifiedTiles.add(Pair(tileX, tileY))
        }
    }

    // Save all modified tiles to storage
    fun saveModifiedTiles() {
        synchronized(modifiedTiles) {
            modifiedTiles.forEach { (tileX, tileY) ->
                val tile = tileCache[Pair(tileX, tileY)]
                if (tile != null) {
                    saveTile(tile)
                }
            }
            modifiedTiles.clear()
        }
    }

    // Get the directory where tiles are stored
    private fun getTilesDirectory(): File {
        val tilesDir = File(context.filesDir, TILES_DIRECTORY)
        if (!tilesDir.exists()) {
            tilesDir.mkdirs()
        }
        return tilesDir
    }

    // Generate a filename for a tile
    private fun getTileFilename(tileX: Int, tileY: Int): String {
        return "tile_${tileX}_${tileY}.bin"
    }

    // Load a tile from storage
    private fun loadTile(tileX: Int, tileY: Int): Tile? {
        val tilesDir = getTilesDirectory()
        val tileFile = File(tilesDir, getTileFilename(tileX, tileY))

        if (!tileFile.exists()) {
            return null
        }

        try {
            FileInputStream(tileFile).use { inputStream ->
                val expectedSize = TILE_SIZE * TILE_SIZE / 8
                val buffer = ByteArray(expectedSize)
                val bytesRead = inputStream.read(buffer)

                if (bytesRead == expectedSize) {
                    val tile = Tile(tileX, tileY)
                    tile.fromByteArray(buffer)
                    return tile
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    // Save a tile to storage
    private fun saveTile(tile: Tile): Boolean {
        val tilesDir = getTilesDirectory()
        val tileFile = File(tilesDir, getTileFilename(tile.x, tile.y))

        try {
            FileOutputStream(tileFile).use { outputStream ->
                val data = tile.toByteArray()
                outputStream.write(data)
                outputStream.flush()
                return true
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return false
    }

    // Check if a tile exists in storage
    fun tileExists(tileX: Int, tileY: Int): Boolean {
        val tilesDir = getTilesDirectory()
        val tileFile = File(tilesDir, getTileFilename(tileX, tileY))
        return tileFile.exists()
    }
}

// Represents a single 256x256 tile of pixels
class Tile(val x: Int, val y: Int) {
    // Store pixels as a 2D array of bits
    // We're using BooleanArray for simplicity; in a more optimized implementation,
    // we would pack these into a BitSet or similar structure for memory efficiency
    private val pixels = Array(TileManager.TILE_SIZE) { BooleanArray(TileManager.TILE_SIZE) }

    // Check if a pixel is filled (true = black, false = white)
    fun getPixel(x: Int, y: Int): Boolean {
        if (x < 0 || x >= TileManager.TILE_SIZE || y < 0 || y >= TileManager.TILE_SIZE) {
            return false
        }
        return pixels[y][x]
    }

    // Set a pixel to filled or not
    fun setPixel(x: Int, y: Int, filled: Boolean) {
        if (x < 0 || x >= TileManager.TILE_SIZE || y < 0 || y >= TileManager.TILE_SIZE) {
            return
        }
        pixels[y][x] = filled
    }

    // Convert to byte array for storage
    fun toByteArray(): ByteArray {
        // Calculate the number of bytes needed to store all pixels
        // Each pixel is 1 bit, so we need (TILE_SIZE * TILE_SIZE) / 8 bytes
        val numBytes = TileManager.TILE_SIZE * TileManager.TILE_SIZE / 8
        val result = ByteArray(numBytes)

        var byteIndex = 0
        var bitPosition = 7

        for (y in 0 until TileManager.TILE_SIZE) {
            for (x in 0 until TileManager.TILE_SIZE) {
                // If the pixel is filled, set the corresponding bit
                if (pixels[y][x]) {
                    result[byteIndex] = (result[byteIndex].toInt() or (1 shl bitPosition)).toByte()
                }

                // Move to the next bit position
                bitPosition--
                if (bitPosition < 0) {
                    bitPosition = 7
                    byteIndex++
                }
            }
        }

        return result
    }

    // Initialize from byte array (from storage)
    fun fromByteArray(data: ByteArray) {
        var byteIndex = 0
        var bitPosition = 7

        for (y in 0 until TileManager.TILE_SIZE) {
            for (x in 0 until TileManager.TILE_SIZE) {
                // Check if this bit is set
                val byteValue = data[byteIndex].toInt() and 0xFF
                val filled = (byteValue and (1 shl bitPosition)) != 0
                pixels[y][x] = filled

                // Move to the next bit position
                bitPosition--
                if (bitPosition < 0) {
                    bitPosition = 7
                    byteIndex++
                }
            }
        }
    }
}