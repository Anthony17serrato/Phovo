package com.serratocreations.phovo.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals

class DebDependenciesTest {

    @Test
    fun `t64 packages also accept their old names`() {
        // The relevant part of the first release run's Depends field.
        assertEquals(
            "libasound2t64 | libasound2, libbrotli1, libc6, libpng16-16t64 | libpng16-16, xdg-utils, zlib1g",
            widenT64Dependencies(" libasound2t64, libbrotli1, libc6, libpng16-16t64, xdg-utils, zlib1g")
        )
    }

    @Test
    fun `version constraint stays on the t64 name only`() {
        // Bookworm ships libasound2 1.2.8; carrying ">= 1.2.11" over would make the fallback unusable.
        assertEquals(
            "libasound2t64 (>= 1.2.11) | libasound2",
            widenT64Dependencies("libasound2t64 (>= 1.2.11)")
        )
    }

    @Test
    fun `existing alternatives and other packages are left alone`() {
        assertEquals("libfoot64 | libfoo, libc6 (>= 2.34)", widenT64Dependencies("libfoot64 | libfoo, libc6 (>= 2.34)"))
    }
}
