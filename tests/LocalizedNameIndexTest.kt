package com.example.commanderlens

import java.io.StringReader

fun main() {
    val index = LocalizedNameIndex.fromTsv(StringReader("""
        it\tAnello Solare\tSol Ring
        es\tAnillo solar\tSol Ring
        it\tIsola\tIsland
        es\tIsla\tIsland
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
    check(index.resolve("Montana")?.englishName == "Mountain")
    check(index.resolve("Anello Solare", "es") == null)
    check(index.resolve("Anillo solar", "it") == null)
    check(index.resolve("Campo") == null) // ambiguous localized name
    check(index.resolve("Unknown Card") == null)
    println("PASS: Italian/Spanish OCR names and canonical English resolution")
}
