package com.example.hydrotracker.logic

enum class TipSourceKind(val icon: String) {
    STUDY("📄"), REVIEW("📚"), GUIDELINE("📋"), RESOURCE("🏛")
}

data class TipSource(val title: String, val url: String, val kind: TipSourceKind)

private fun pubmedSearch(query: String) =
    "https://pubmed.ncbi.nlm.nih.gov/?term=" + query.trim().replace(" ", "+")

private val ACSM = TipSource(
    "Sawka et al. — ACSM Position Stand: Exercise and Fluid Replacement (MSSE 2007)",
    "https://pubmed.ncbi.nlm.nih.gov/17277604/", TipSourceKind.GUIDELINE)
private val POPKIN = TipSource(
    "Popkin, D'Anci & Rosenberg — Water, hydration and health (Nutr Rev 2010)",
    pubmedSearch("Popkin D'Anci Rosenberg water hydration health physiological role adults"),
    TipSourceKind.REVIEW)
private val JEQUIER = TipSource(
    "Jéquier & Constant — Water as an essential nutrient (Eur J Clin Nutr 2010)",
    pubmedSearch("Jequier Constant water essential nutrient physiological basis hydration"),
    TipSourceKind.REVIEW)
private val EFSA_DOI = TipSource(
    "EFSA — Dietary Reference Values for water (EFSA Journal 2010)",
    "https://doi.org/10.2903/j.efsa.2010.1459", TipSourceKind.GUIDELINE)
private val NAP_DRI = TipSource(
    "NASEM — Dietary Reference Intakes for Water, Potassium, Sodium… (2005)",
    "https://nap.nationalacademies.org/catalog/10925/", TipSourceKind.GUIDELINE)
private val NIDDK_STONES = TipSource(
    "NIDDK (NIH) — Kidney Stones: Eating, Diet & Nutrition",
    "https://www.niddk.nih.gov/health-information/urologic-diseases/kidney-stones",
    TipSourceKind.RESOURCE)
private val NIDDK_UTI = TipSource(
    "NIDDK (NIH) — Bladder Infection (UTI) in Adults",
    "https://www.niddk.nih.gov/health-information/urologic-diseases/bladder-infection-uti-adults",
    TipSourceKind.RESOURCE)
private val NIDDK_CONSTIPATION = TipSource(
    "NIDDK (NIH) — Constipation: Treatment (fluid & fiber guidance)",
    "https://www.niddk.nih.gov/health-information/digestive-diseases/constipation",
    TipSourceKind.RESOURCE)
private val NCCIH_DETOX = TipSource(
    "NIH NCCIH — \"Detoxes\" and \"Cleanses\": What You Need to Know",
    "https://www.nccih.nih.gov/health/detoxes-and-cleanses", TipSourceKind.RESOURCE)
private val MAYO_DEHYD = TipSource(
    "Mayo Clinic — Dehydration: Symptoms & Causes",
    "https://www.mayoclinic.org/diseases-conditions/dehydration/symptoms-causes/syc-20354086",
    TipSourceKind.RESOURCE)
private val MAYO_DRYMOUTH = TipSource(
    "Mayo Clinic — Dry Mouth: Symptoms & Causes",
    "https://www.mayoclinic.org/diseases-conditions/dry-mouth/symptoms-causes/syc-20356048",
    TipSourceKind.RESOURCE)
private val NHS_WATER = TipSource(
    "NHS — Water, drinks and hydration",
    "https://www.nhs.uk/live-well/eat-well/food-guidelines-and-food-standards/water-drinks-nutrition/",
    TipSourceKind.RESOURCE)
private val CDC_YELLOWBOOK = TipSource(
    "CDC Yellow Book — Travel Health (altitude & air-travel chapters)",
    "https://wwwnc.cdc.gov/travel/yellowbook", TipSourceKind.RESOURCE)
private val CDC_WATER = TipSource(
    "CDC — Safe Water for Travel & Outdoors",
    "https://www.cdc.gov/drinking-water/", TipSourceKind.RESOURCE)
private val USGS_BODY = TipSource(
    "USGS Water Science School — The Water in You",
    "https://www.usgs.gov/special-topics/water-science-school/science/water-you-water-and-human-body",
    TipSourceKind.RESOURCE)
private val DRINKAWARE = TipSource(
    "Drinkaware (UK) — Alcohol & dehydration",
    "https://www.drinkaware.co.uk/", TipSourceKind.RESOURCE)
private val KENEFICK_WORK = TipSource(
    "Kenefick & Sawka — Hydration at the work site (J Am Coll Nutr 2007)",
    pubmedSearch("Kenefick Sawka hydration work site"), TipSourceKind.REVIEW)
private val LALLY = TipSource(
    "Lally et al. — How are habits formed? (Eur J Soc Psychol 2010)",
    pubmedSearch("Lally van Jaarsveld Potts Wardle how are habits formed"), TipSourceKind.STUDY)
private val ARMSTRONG_URINE = TipSource(
    "Armstrong et al. — Urine color as a hydration index",
    pubmedSearch("Armstrong urine color hydration assessment index exercise"), TipSourceKind.STUDY)

val TIP_SOURCES: Map<String, List<TipSource>> = mapOf(
    "wake" to listOf(POPKIN, JEQUIER),
    "coffee" to listOf(
        TipSource("Killer et al. — No evidence of dehydration with moderate coffee intake (PLOS ONE 2014)",
            "https://pubmed.ncbi.nlm.nih.gov/24416202/", TipSourceKind.STUDY)),
    "food" to listOf(EFSA_DOI),
    "thirst" to listOf(
        TipSource("Greenleaf — Problem: thirst, drinking behavior, involuntary dehydration (MSSE 1992)",
            pubmedSearch("Greenleaf thirst drinking behavior involuntary dehydration"),
            TipSourceKind.REVIEW)),
    "preload" to listOf(ACSM),
    "twopct" to listOf(ACSM),
    "salt" to listOf(ACSM),
    "urine" to listOf(ARMSTRONG_URINE, MAYO_DEHYD),
    "winter" to listOf(
        TipSource("Kenefick et al. — Cold-induced diuresis & hydration in cold environments",
            pubmedSearch("Kenefick cold induced diuresis hydration cold environment"),
            TipSourceKind.STUDY)),
    "flight" to listOf(CDC_YELLOWBOOK),
    "oneforone" to listOf(DRINKAWARE,
        TipSource("Alcohol & vasopressin-mediated diuresis (physiology review)",
            pubmedSearch("alcohol diuresis vasopressin suppression mechanism review"),
            TipSourceKind.REVIEW)),
    "temp" to listOf(
        TipSource("Szlyk et al. — Water temperature drives drinking behavior",
            pubmedSearch("Szlyk water temperature drinking dehydration"), TipSourceKind.STUDY)),
    "sick" to listOf(MAYO_DEHYD),
    "altitude" to listOf(CDC_YELLOWBOOK),
    "slump" to listOf(
        TipSource("Ganio et al. — Mild dehydration impairs cognitive performance & mood of men (Br J Nutr 2011)",
            pubmedSearch("Ganio mild dehydration impairs cognitive performance mood men"),
            TipSourceKind.STUDY),
        TipSource("Armstrong et al. — Mild dehydration affects mood in young women (J Nutr 2012)",
            pubmedSearch("Armstrong mild dehydration affects mood healthy young women"),
            TipSourceKind.STUDY)),
    "flavor" to listOf(NHS_WATER),
    "tea" to listOf(POPKIN),
    "stack" to listOf(LALLY),
    "sip" to listOf(JEQUIER),
    "visible" to listOf(LALLY),
    "heat" to listOf(ACSM, MAYO_DEHYD),
    "bedtime" to listOf(NHS_WATER),
    "hypo" to listOf(
        TipSource("Almond et al. — Hyponatremia among runners in the Boston Marathon (NEJM 2005)",
            "https://pubmed.ncbi.nlm.nih.gov/15829535/", TipSourceKind.STUDY)),
    "eightglasses" to listOf(
        TipSource("Valtin — \"Drink at least 8 glasses a day.\" Really? (Am J Physiol 2002)",
            "https://pubmed.ncbi.nlm.nih.gov/12119224/", TipSourceKind.REVIEW), POPKIN),
    "lemonmyth" to listOf(NHS_WATER),
    "detox" to listOf(NCCIH_DETOX),
    "skin" to listOf(
        TipSource("Palma et al. — Dietary water affects human skin hydration (2015)",
            pubmedSearch("Palma dietary water affects human skin hydration biomechanics"),
            TipSourceKind.STUDY)),
    "desksalt" to listOf(ACSM),
    "hunger" to listOf(POPKIN),
    "mood" to listOf(
        TipSource("Armstrong et al. — Mild dehydration affects mood (J Nutr 2012)",
            pubmedSearch("Armstrong mild dehydration affects mood healthy young women"),
            TipSourceKind.STUDY)),
    "premeal" to listOf(
        TipSource("Dennis et al. — Water consumption increases weight loss in a hypocaloric diet (Obesity 2010)",
            pubmedSearch("Dennis water consumption increases weight loss hypocaloric diet intervention"),
            TipSourceKind.STUDY),
        TipSource("Parretti et al. — Water preloading before meals (RCT)",
            pubmedSearch("Parretti water preloading adults randomized controlled trial"),
            TipSourceKind.STUDY)),
    "bathroom" to listOf(ARMSTRONG_URINE, MAYO_DEHYD),
    "bodywater" to listOf(USGS_BODY),
    "stones" to listOf(NIDDK_STONES,
        TipSource("Borghi et al. — Urine volume & recurrences in calcium nephrolithiasis, 5-yr RCT (J Urol 1996)",
            pubmedSearch("Borghi urine volume water recurrences idiopathic calcium nephrolithiasis"),
            TipSourceKind.STUDY)),
    "uti" to listOf(
        TipSource("Hooton et al. — Increased water intake in women with recurrent cystitis (JAMA Intern Med 2018)",
            pubmedSearch("Hooton increased daily water intake premenopausal women recurrent cystitis"),
            TipSourceKind.STUDY), NIDDK_UTI),
    "fiber" to listOf(NIDDK_CONSTIPATION),
    "meds" to listOf(MAYO_DEHYD),
    "older" to listOf(
        TipSource("Kenney & Chiu — Influence of age on thirst and fluid intake (MSSE 2001)",
            pubmedSearch("Kenney Chiu influence age thirst fluid intake"), TipSourceKind.REVIEW), POPKIN),
    "kids" to listOf(EFSA_DOI),
    "pregnancy" to listOf(NAP_DRI),
    "sportsdrink" to listOf(ACSM),
    "cramps" to listOf(
        TipSource("Schwellnus — Exercise-associated muscle cramps: altered neuromuscular control (BJSM)",
            pubmedSearch("Schwellnus exercise associated muscle cramps altered neuromuscular control"),
            TipSourceKind.REVIEW)),
    "voluntary" to listOf(ACSM,
        TipSource("Greenleaf — Involuntary dehydration (MSSE 1992)",
            pubmedSearch("Greenleaf thirst drinking behavior involuntary dehydration"),
            TipSourceKind.REVIEW)),
    "sweatrange" to listOf(ACSM),
    "recoverytime" to listOf(
        TipSource("Shirreffs et al. — Post-exercise rehydration: effect of volume consumed (Eur J Appl Physiol 1996)",
            pubmedSearch("Shirreffs post exercise rehydration man effects volume consumed"),
            TipSourceKind.STUDY)),
    "streams" to listOf(CDC_WATER),
    "pool" to listOf(ACSM),
    "ski" to listOf(CDC_YELLOWBOOK),
    "roadtrip" to listOf(NHS_WATER),
    "ac" to listOf(KENEFICK_WORK),
    "meeting" to listOf(KENEFICK_WORK),
    "hangover" to listOf(
        TipSource("Pittler et al. — Interventions for preventing/treating alcohol hangover (BMJ 2005)",
            pubmedSearch("Pittler Verster Ernst interventions preventing treating alcohol hangover systematic review"),
            TipSourceKind.STUDY)),
    "drymouth" to listOf(MAYO_DRYMOUTH),
)
