@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class IconChoice(val key: String, val label: String, val icon: ImageVector)
object IconRegistry {
    val choices: List<IconChoice> = listOf(
        IconChoice("basket", "বাজার", Icons.Default.ShoppingBasket),
        IconChoice("bag", "ব্যাগ", Icons.Default.ShoppingBag),
        IconChoice("restaurant", "খাবার", Icons.Default.Restaurant),
        IconChoice("fastfood", "ফাস্টফুড", Icons.Default.Fastfood),
        IconChoice("cafe", "ক্যাফে", Icons.Default.LocalCafe),
        IconChoice("dining", "রেস্তোরাঁ", Icons.Default.LocalDining),
        IconChoice("cake", "কেক", Icons.Default.Cake),
        IconChoice("icecream", "আইসক্রিম", Icons.Default.Icecream),
        IconChoice("pizza", "পিৎজা", Icons.Default.LocalPizza),
        IconChoice("water", "পানি", Icons.Default.WaterDrop),
        IconChoice("home", "বাসা", Icons.Default.Home),
        IconChoice("apartment", "ফ্ল্যাট", Icons.Default.Apartment),
        IconChoice("bed", "শয়নকক্ষ", Icons.Default.Bed),
        IconChoice("bathtub", "বাথরুম", Icons.Default.Bathtub),
        IconChoice("chair", "আসবাব", Icons.Default.Chair),
        IconChoice("cleaning", "পরিষ্কার", Icons.Default.CleaningServices),
        IconChoice("build", "মেরামত", Icons.Default.Build),
        IconChoice("lightbulb", "বাতি", Icons.Default.Lightbulb),
        IconChoice("receipt", "বিল", Icons.Default.ReceiptLong),
        IconChoice("bolt", "বিদ্যুৎ", Icons.Default.Bolt),
        IconChoice("wifi", "ইন্টারনেট", Icons.Default.Wifi),
        IconChoice("phone", "ফোন", Icons.Default.PhoneAndroid),
        IconChoice("smartphone", "মোবাইল", Icons.Default.Smartphone),
        IconChoice("sim", "সিম", Icons.Default.SimCard),
        IconChoice("bus", "বাস", Icons.Default.DirectionsBus),
        IconChoice("car", "গাড়ি", Icons.Default.DirectionsCar),
        IconChoice("bike", "বাইক", Icons.Default.PedalBike),
        IconChoice("train", "ট্রেন", Icons.Default.Train),
        IconChoice("flight", "বিমান", Icons.Default.Flight),
        IconChoice("boat", "নৌকা", Icons.Default.DirectionsBoat),
        IconChoice("taxi", "ট্যাক্সি", Icons.Default.LocalTaxi),
        IconChoice("fuel", "জ্বালানি", Icons.Default.LocalGasStation),
        IconChoice("parking", "পার্কিং", Icons.Default.LocalParking),
        IconChoice("medical", "মেডিকেল", Icons.Default.MedicalServices),
        IconChoice("hospital", "হাসপাতাল", Icons.Default.LocalHospital),
        IconChoice("pill", "ওষুধ", Icons.Default.Medication),
        IconChoice("vaccine", "টিকা", Icons.Default.Vaccines),
        IconChoice("fitness", "ব্যায়াম", Icons.Default.FitnessCenter),
        IconChoice("spa", "স্পা", Icons.Default.Spa),
        IconChoice("school", "শিক্ষা", Icons.Default.School),
        IconChoice("book", "বই", Icons.Default.MenuBook),
        IconChoice("library", "লাইব্রেরি", Icons.Default.LocalLibrary),
        IconChoice("edit", "লেখালেখি", Icons.Default.Edit),
        IconChoice("science", "বিজ্ঞান", Icons.Default.Science),
        IconChoice("calculate", "হিসাব", Icons.Default.Calculate),
        IconChoice("wallet", "ওয়ালেট", Icons.Default.AccountBalanceWallet),
        IconChoice("cash", "নগদ", Icons.Default.Payments),
        IconChoice("bank", "ব্যাংক", Icons.Default.AccountBalance),
        IconChoice("card", "কার্ড", Icons.Default.CreditCard),
        IconChoice("savings", "সঞ্চয়", Icons.Default.Savings),
        IconChoice("trending", "আয়", Icons.Default.TrendingUp),
        IconChoice("store", "দোকান", Icons.Default.Storefront),
        IconChoice("business", "ব্যবসা", Icons.Default.BusinessCenter),
        IconChoice("work", "কাজ", Icons.Default.Work),
        IconChoice("gift", "উপহার", Icons.Default.CardGiftcard),
        IconChoice("heart", "দান", Icons.Default.VolunteerActivism),
        IconChoice("favorite", "প্রিয়", Icons.Default.Favorite),
        IconChoice("child", "শিশু", Icons.Default.ChildCare),
        IconChoice("pets", "পোষা প্রাণী", Icons.Default.Pets),
        IconChoice("sports", "খেলা", Icons.Default.SportsEsports),
        IconChoice("movie", "সিনেমা", Icons.Default.Movie),
        IconChoice("music", "সঙ্গীত", Icons.Default.MusicNote),
        IconChoice("travel", "ভ্রমণ", Icons.Default.Luggage),
        IconChoice("beach", "ছুটি", Icons.Default.BeachAccess),
        IconChoice("check", "টুডু", Icons.Default.Checklist),
        IconChoice("target", "লক্ষ্য", Icons.Default.GpsFixed),
        IconChoice("more", "অন্যান্য", Icons.Default.Category)
    )
    fun get(key: String): ImageVector = choices.firstOrNull { it.key == key }?.icon ?: Icons.Default.Category
    fun parseColor(hex: String, fallback: Color = Color(0xFF145443)): Color = try {
        val clean = hex.trim().removePrefix("#")
        require(clean.length == 6)
        Color(0xFF000000 or clean.toLong(16))
    } catch (_: Exception) { fallback }
}
