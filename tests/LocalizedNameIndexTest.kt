package com.example.commanderlens

import java.io.StringReader

fun main() {
    val index = LocalizedNameIndex.fromTsv(StringReader("""
        it\tAnello Solare\tSol Ring
        es\tAnillo solar\tSol Ring
        it\tIsola\tIsland
        it\tIsola\tIsland // Island
        es\tIsla\tIsland
        es\tIsla\tIsland // Island
        it\tPalude\tSwamp
        it\tPalude\tSwamp // Swamp
        es\tPantano\tSwamp
        es\tPantano\tSwamp // Swamp
        it\tPianura\tPlains
        it\tPianura\tPlains // Plains
        it\tMontagna\tMountain
        it\tMontagna\tMountain // Mountain
        es\tMontaña\tMountain
        it\tMontagna\tMountain
        it\tUn'ora di prova\tTest Hour
        es\tCampo\tCard A
        es\tCampo\tCard B
    """.trimIndent().replace("\\t", "\t")))
    check(index.resolve("Anello Solare")?.englishName == "Sol Ring")
    check(index.resolve("Anillo solar")?.englishName == "Sol Ring")
    check(index.resolve("ANILLO SÓLAR")?.englishName == "Sol Ring")
    check(index.resolve("Anillo soler")?.englishName == "Sol Ring")
    check(index.resolve("Isola")?.englishName == "Island")
    check(index.resolve("Isla")?.englishName == "Island")
    check(index.resolve("Palude", "it")?.englishName == "Swamp")
    check(index.resolve("Palude")?.englishName == "Swamp")
    check(index.resolve("Pantano", "es")?.englishName == "Swamp")
    check(index.resolve("Pianura")?.englishName == "Plains")
    check(index.resolve("Montagna")?.englishName == "Mountain")
    check(collapseSameFaceName("Swamp // Swamp") == "Swamp")
    check(collapseSameFaceName("Fire // Ice") == "Fire // Ice")
    check(index.resolve("Montana")?.englishName == "Mountain")
    check(index.resolve("Anello Solare", "es") == null)
    check(index.resolve("Anillo solar", "it") == null)
    check(index.resolve("Campo") == null) // ambiguous localized name
    check(index.resolve("Unknown Card") == null)
    println("PASS: Italian/Spanish OCR names and canonical English resolution")
}
