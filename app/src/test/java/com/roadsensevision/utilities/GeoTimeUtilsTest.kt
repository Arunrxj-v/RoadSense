package com.roadsensevision.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoTimeUtilsTest {

    @Test
    fun `distance from a point to itself is zero`() {
        assertEquals(0.0, GeoUtils.distanceMeters(12.34, 56.78, 12.34, 56.78), 1e-6)
    }

    @Test
    fun `one degree of latitude is about 111 km`() {
        // R = 6371000 m -> 1 degree = R * pi/180 = 111194.93 m
        val d = GeoUtils.distanceMeters(0.0, 0.0, 1.0, 0.0)
        assertEquals(111_194.93, d, 1.0)
    }

    @Test
    fun `distance is symmetric`() {
        val a = GeoUtils.distanceMeters(40.7128, -74.0060, 51.5074, -0.1278)
        val b = GeoUtils.distanceMeters(51.5074, -0.1278, 40.7128, -74.0060)
        assertEquals(a, b, 1e-6)
    }

    @Test
    fun `transatlantic distance is in the right ballpark`() {
        // NYC -> London is roughly 5570 km.
        val d = GeoUtils.distanceMeters(40.7128, -74.0060, 51.5074, -0.1278)
        assertTrue("was $d", d in 5_500_000.0..5_650_000.0)
    }

    @Test
    fun `formatCoordinate handles missing fixes without inventing coordinates`() {
        assertEquals("Unavailable", GeoUtils.formatCoordinate(null, null))
        assertEquals("Unavailable", GeoUtils.formatCoordinate(12.0, null))
        assertEquals("Unavailable", GeoUtils.formatCoordinate(null, 12.0))
    }

    @Test
    fun `formatCoordinate renders hemisphere directions`() {
        val s = GeoUtils.formatCoordinate(40.7128, -74.0060)
        assertTrue(s, s.contains("N"))
        assertTrue(s, s.contains("W"))
        assertTrue(s, s.contains("40.71280"))
    }

    @Test
    fun `formatAccuracy handles missing accuracy`() {
        assertEquals("± —", GeoUtils.formatAccuracy(null))
        assertEquals("±5 m", GeoUtils.formatAccuracy(5f))
    }

    @Test
    fun `formatDuration renders minutes and hours`() {
        assertEquals("00:00", TimeUtils.formatDuration(0L))
        assertEquals("00:05", TimeUtils.formatDuration(5_000L))
        assertEquals("01:05", TimeUtils.formatDuration(65_000L))
        assertEquals("1:01:01", TimeUtils.formatDuration(3_661_000L))
    }

    @Test
    fun `formatMinutes converts milliseconds`() {
        assertEquals(0, TimeUtils.formatMinutes(0L))
        assertEquals(2, TimeUtils.formatMinutes(120_000L))
        assertEquals(1, TimeUtils.formatMinutes(119_999L))
    }
}
