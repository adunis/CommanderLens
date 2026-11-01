package com.example.commanderlens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.StringReader

class LocalizedNameIndexJvmTest {
    private val example = """
        it\tPalude\tSwamp
        it\tPalude\tSwamp // Swamp
        it\tIsola\tIsland
        it\tIsola\tIsland // Island
        it\tPianura\tPlains
        it\tPianura\tPlains // Plains
        it\tForesta\tForest
        it\tForesta\tForest // Forest
        it\tMontagna\tMountain
        it\tMontagna\tMountain // Mountain
        es\tPantano\tSwamp
        es\tPantano\tSwamp // Swamp
        es\tIsla\tIsland
        es\tIsla\tIsland // Island
        es\tBosque\tForest
        es\tBosque\tForest // Forest
        es\tLlanura\tPlains
        es\tLlanura\tPlains // Plains
        es\tMontaña\tMountain
        es\tMontaña\tMountain // Mountain
        it\tPantano\tQuagmire
        es\tCampo\tCard A
        es\tCampo\tCard B
    """.trimIndent().replace("\\t", "\t")

    @Test fun basicLandsRecognizedInItalian() {
        val index = LocalizedNameIndex.fromTsv(StringReader(example))
        mapOf("Palude" to "Swamp", "Isola" to "Island",
            "Foresta" to "Forest", "Pianura" to "Plains",
            "Montagna" to "Mountain").forEach { (printed, canonical) ->
            assertEquals(printed, canonical, index.resolve(printed, "it")?.englishName)
        }
        assertEquals("Swamp", index.resolve("Palude")?.englishName)
    }

    @Test fun basicLandsRecognizedInSpanish() {
        val index = LocalizedNameIndex.fromTsv(StringReader(example))
        mapOf("Pantano" to "Swamp", "Isla" to "Island",
            "Bosque" to "Forest", "Llanura" to "Plains",
            "Montaña" to "Mountain").forEach { (printed, canonical) ->
            assertEquals(printed, canonical, index.resolve(printed, "es")?.englishName)
        }
        assertEquals("Mountain", index.resolve("Montana", "es")?.englishName)
    }

    @Test fun trulyAmbiguousNamesAreNotGuessed() {
        val index = LocalizedNameIndex.fromTsv(StringReader(example))
        assertNull(index.resolve("Pantano"))
        assertNull(index.resolve("Campo", "es"))
        assertEquals("Swamp", index.resolve("Pantano", "es")?.englishName)
        assertEquals("Quagmire", index.resolve("Pantano", "it")?.englishName)
    }

    @Test fun onlyIdenticalCardFacesCollapse() {
        assertEquals("Swamp", collapseSameFaceName("Swamp // Swamp"))
        assertEquals("Fire // Ice", collapseSameFaceName("Fire // Ice"))
    }
}
