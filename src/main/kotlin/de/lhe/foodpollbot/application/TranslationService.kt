package de.lhe.foodpollbot.application

import net.mamoe.yamlkt.Yaml
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.random.Random

const val HELP_KEY = "help"
const val GET_IN_KEY = "getIn"
const val GET_OUT_KEY = "getOut"
const val NAMED_FOOD_POLL_KEY = "namedFoodPoll"
const val FOOD_POLL_KEY = "foodPoll"
const val FOOD_POLL_START_KEY = "foodPollStart"
const val NAMED_FOOD_POLL_START_KEY = "namedFoodPollStart"
const val FOOD_POLL_CANCELED_KEY = "foodPollCanceled"
const val NAMED_FOOD_POLL_CANCELED_KEY = "namedFoodPollCanceled"
const val TIME_FORMAT_ERROR_KEY = "timeFormatError"
const val TIME_IN_PAST_ERROR_KEY = "timeInPastError"
const val TIME_EXISTS_ERROR_KEY = "timeExistsError"

val translationFileRegex = "translations-(?<name>.*)\\.yaml".toRegex()
val translationsPath = System.getenv("TRANSLATIONS_PATH") ?: "translations"

typealias FoodPollTranslations = Map<String?, Any?>

val translations = HashMap<String, FoodPollTranslations>()

fun resetTranslationCache() = translations.clear()

fun getFoodPollTypes(): List<String> {
    return Files.walk(Paths.get(translationsPath))
        .map { it.name }
        .filter { it matches translationFileRegex }
        .map { translationFileRegex.find(it)!!.groups["name"]!!.value }
        .distinct()
        .toList()
}

fun getTranslatedMessage(foodPollType: String, messageKey: String, messageVariant: Int? = null, vararg messageArgs: String): String {
    val possibleTranslations = getTranslationForType(foodPollType)[messageKey]
    var translation = if (possibleTranslations is List<*>) {
        val variant = messageVariant ?: Random.nextInt(0, possibleTranslations.size - 1)
        possibleTranslations[variant].toString()
    } else {
        possibleTranslations.toString()
    }
    messageArgs.forEach {
        translation = translation.replaceFirst("{}", it)
    }
    return translation
}

fun getNumOfPossibleTranslations(foodPollType: String, messageKey: String): Int {
    val possibleTranslations = getTranslationForType(foodPollType)[messageKey]
    return if (possibleTranslations is List<*>) possibleTranslations.size else 1
}

private fun getTranslationForType(foodPollType: String): FoodPollTranslations =
    translations[foodPollType] ?: loadTranslations(foodPollType)

private fun loadTranslations(foodPollType: String): FoodPollTranslations {
    val translationsFile = Files.readString(Paths.get(translationsPath, "translations-$foodPollType.yaml"))
    if (translationsFile == null) {
        throw IllegalArgumentException("Food poll type $foodPollType does not exist")
    } else {
        val translation = Yaml.decodeMapFromString(translationsFile)
        translations[foodPollType] = translation
        return translation
    }
}
