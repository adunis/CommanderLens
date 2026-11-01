package com.example.commanderlens

/** Run with: kotlinc ScanGate.kt DeckRules.kt ScannerRulesTest.kt -include-runtime -d rules.jar && java -jar rules.jar */
fun main() {
    val gate = ScanGate(stableFrames = 3, emptyFramesToUnlock = 3)
    check(gate.observe("Sol Ring") == null)
    check(gate.observe("Sol Ring") == null)
    check(gate.observe("Sol Ring") == "Sol Ring")
    gate.accepted("Sol Ring")
    repeat(5) { check(gate.observe("Sol Ring") == null) }
    repeat(2) { check(gate.observe(null) == null) }
    check(gate.observe("Sol Ring") == null)
    check(gate.observe("Sol Ring") == null)
    check(gate.observe("Sol Ring") == null) // two empty frames are insufficient
    repeat(3) { gate.observe(null) }
    check(gate.observe("Sol Ring") == null)
    check(gate.observe("Sol Ring") == null)
    check(gate.observe("Sol Ring") == "Sol Ring")
    gate.accepted("Sol Ring")
    repeat(3) { check(gate.observe("Lightning Bolt") == (if (it == 2) "Lightning Bolt" else null)) }
    check(DeckRules.isBasicLand("Basic Land — Island"))
    check(DeckRules.isBasicLand("Basic Snow Land — Island"))
    check(!DeckRules.isBasicLand("Legendary Land — Urza's Saga"))
    check(DeckRules.mayAdd(99, true, 2))
    check(DeckRules.mayAdd(1, false, 2))
    check(!DeckRules.mayAdd(2, false, 2))
    check(!DeckRules.mayAdd(1, false, 1))
    check(plausibleCardName("Fable of the Mirror-Breaker", "Fable of the Mirror-Breaker // Reflection of Kiki-Jiki"))
    check(plausibleCardName("RefIection of Kiki-Jiki", "Fable of the Mirror-Breaker // Reflection of Kiki-Jiki"))
    check(!plausibleCardName("Dragon", "Ancient Copper Dragon"))
    println("PASS: scan stabilization, duplicate handling, copy limits and fuzzy matching")
}
