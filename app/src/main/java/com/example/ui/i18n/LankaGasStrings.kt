package com.example.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.model.AppLanguage
import com.example.data.model.QueueLevel
import com.example.data.model.SriLankaDistrict
import com.example.data.model.StockStatus

data class TrilingualStrings(
    val language: AppLanguage,
    val appTitle: String,
    val tagline: String,
    val signInSubtitle: String,
    val signInButton: String,
    val tabExplore: String,
    val tabSeller: String,
    val tabAdmin: String,
    val searchPlaceholder: String,
    val mapMode: String,
    val listMode: String,
    val allBrands: String,
    val availableOnly: String,
    val allDistricts: String,
    val statusAvailable: String,
    val statusInTransit: String,
    val statusScheduled: String,
    val statusSoldOut: String,
    val queueNone: String,
    val queueShort: String,
    val queueModerate: String,
    val queueLong: String,
    val verifiedDealer: String,
    val unverifiedDealer: String,
    val liveCylinderStock: String,
    val queueTelemetry: String,
    val peopleInQueue: String,
    val estWaitMins: String,
    val trustScore: String,
    val watchRestock: String,
    val watchingRestock: String,
    val directions: String,
    val callDealer: String,
    val submitQueueReport: String,
    val confirmStockAccurate: String,
    val sellerDashboardTitle: String,
    val registerNewOutlet: String,
    val publishStockUpdate: String,
    val markSoldOutNow: String,
    val adminConsoleTitle: String,
    val crowdDiscrepancyWarning: String
) {
    fun statusLabel(status: StockStatus): String = when (status) {
        StockStatus.AVAILABLE -> statusAvailable
        StockStatus.IN_TRANSIT -> statusInTransit
        StockStatus.SCHEDULED -> statusScheduled
        StockStatus.SOLD_OUT -> statusSoldOut
    }

    fun queueLabel(level: QueueLevel): String = when (level) {
        QueueLevel.NONE -> queueNone
        QueueLevel.SHORT -> queueShort
        QueueLevel.MODERATE -> queueModerate
        QueueLevel.LONG -> queueLong
    }

    fun districtLabel(district: SriLankaDistrict): String = when (language) {
        AppLanguage.EN -> district.name
        AppLanguage.SI -> district.sinhalaName
        AppLanguage.TA -> district.tamilName
    }
}

object LankaGasTranslations {
    val english = TrilingualStrings(
        language = AppLanguage.EN,
        appTitle = "LankaGas Live",
        tagline = "Real-Time Litro & LAUGFS Cylinder Tracker",
        signInSubtitle = "Check verified cylinder counts, live lorry dispatches, and community queue wait times across Sri Lanka before leaving home.",
        signInButton = "Sign in with Google",
        tabExplore = "Explore",
        tabSeller = "Seller",
        tabAdmin = "Admin",
        searchPlaceholder = "Search town, road, or dealer...",
        mapMode = "Radar Map",
        listMode = "Nearby List",
        allBrands = "All Brands",
        availableOnly = "In Stock Only",
        allDistricts = "All Sri Lanka",
        statusAvailable = "Available",
        statusInTransit = "In Transit",
        statusScheduled = "Scheduled",
        statusSoldOut = "Sold Out",
        queueNone = "No Queue",
        queueShort = "Short (<15)",
        queueModerate = "Moderate (15–50)",
        queueLong = "Long (50+)",
        verifiedDealer = "Verified Dealer",
        unverifiedDealer = "Pending Verification",
        liveCylinderStock = "Live Cylinder Inventory",
        queueTelemetry = "Queue & Crowd Verification",
        peopleInQueue = "people in line",
        estWaitMins = "min est. wait",
        trustScore = "Trust",
        watchRestock = "Alert on Restock",
        watchingRestock = "Watching Restock",
        directions = "Directions",
        callDealer = "Call Shop",
        submitQueueReport = "Report Queue / Confirm Stock",
        confirmStockAccurate = "Send Live Queue Report",
        sellerDashboardTitle = "Distributor Stock Console",
        registerNewOutlet = "Register Outlet",
        publishStockUpdate = "Publish Live Stock Update",
        markSoldOutNow = "MARK ALL CYLINDERS SOLD OUT",
        adminConsoleTitle = "Island Supply & Trust Moderation",
        crowdDiscrepancyWarning = "Community Alert: Multiple nearby citizens reported this batch Sold Out."
    )

    val sinhala = TrilingualStrings(
        language = AppLanguage.SI,
        appTitle = "LankaGas Live",
        tagline = "සජීවී ලිට්‍රෝ සහ ලාෆ්ස් ගෑස් තොග සහ පෝලිම් තොරතුරු",
        signInSubtitle = "නිවසින් පිටත් වීමට පෙර ඔබ අසල අලෙවිසැල්වල ගෑස් සිලින්ඩර තොග, ලොරි පැමිණෙන වේලාවන් සහ පෝලිම් තත්ත්වය සජීවීව බලන්න.",
        signInButton = "Google ගිණුමෙන් පිවිසෙන්න",
        tabExplore = "සොයන්න",
        tabSeller = "අලෙවිසැල",
        tabAdmin = "පාලනය",
        searchPlaceholder = "නගරය හෝ අලෙවිසැල සොයන්න...",
        mapMode = "සිතියම",
        listMode = "ලැයිස්තුව",
        allBrands = "සියලුම වර්ග",
        availableOnly = "තොග ඇති පමණි",
        allDistricts = "මුළු දිවයිනම",
        statusAvailable = "තොග ඇත",
        statusInTransit = "ප්‍රවාහනයේ",
        statusScheduled = "නියමිතයි",
        statusSoldOut = "අවසන්",
        queueNone = "පෝලිම් නැත",
        queueShort = "කෙටි (<15)",
        queueModerate = "මධ්‍යම (15–50)",
        queueLong = "දිගු (50+)",
        verifiedDealer = "තහවුරු කළ නියෝජිත",
        unverifiedDealer = "තහවුරු කිරීමට නියමිත",
        liveCylinderStock = "සජීවී සිලින්ඩර තොගය",
        queueTelemetry = "පෝලිම් සහ ජනතා තහවුරු කිරීම්",
        peopleInQueue = "දෙනෙක් පෝලිමේ",
        estWaitMins = "මිනිත්තු රැඳී සිටීම",
        trustScore = "විශ්වාසය",
        watchRestock = "තොග ආවිට දන්වන්න",
        watchingRestock = "දැනුම්දීම් සක්‍රියයි",
        directions = "මග පෙන්වීම",
        callDealer = "අමතන්න",
        submitQueueReport = "පෝලිම / තොග තත්ත්වය දන්වන්න",
        confirmStockAccurate = "වාර්තාව යොමු කරන්න",
        sellerDashboardTitle = "නියෝජිත තොග කළමනාකරණය",
        registerNewOutlet = "නව අලෙවිසැලක්",
        publishStockUpdate = "සජීවී තොග යාවත්කාලීන කරන්න",
        markSoldOutNow = "තොග අවසන් බව සලකුණු කරන්න",
        adminConsoleTitle = "දිවයිනේ තොග සහ පාලන පුවරුව",
        crowdDiscrepancyWarning = "ජනතා අවවාදය: අසල සිටින කිහිප දෙනෙකු තොග අවසන් බව වාර්තා කර ඇත."
    )

    val tamil = TrilingualStrings(
        language = AppLanguage.TA,
        appTitle = "LankaGas Live",
        tagline = "லிட்ரோ & லாஃப்ஸ் எரிவாயு நேரடி இருப்பு தகவல்",
        signInSubtitle = "வீட்டை விட்டு புறப்படும் முன் அருகிலுள்ள முகவர்களின் சிலிண்டர் இருப்பு, லொறி வருகை நேரம் மற்றும் வரிசை நிலையை அறியவும்.",
        signInButton = "Google மூலம் உள்நுழையவும்",
        tabExplore = "தேடல்",
        tabSeller = "விற்பனையாளர்",
        tabAdmin = "நிர்வாகம்",
        searchPlaceholder = "நகரம் அல்லது முகவரைத் தேடுங்கள்...",
        mapMode = "வரைபடம்",
        listMode = "பட்டியல்",
        allBrands = "அனைத்தும்",
        availableOnly = "இருப்பு மட்டும்",
        allDistricts = "முழு இலங்கை",
        statusAvailable = "இருப்பு உள்ளது",
        statusInTransit = "வழியில் உள்ளது",
        statusScheduled = "திட்டமிடப்பட்டது",
        statusSoldOut = "விற்றுவிட்டது",
        queueNone = "வரிசை இல்லை",
        queueShort = "குறைவு (<15)",
        queueModerate = "நடுத்தரம் (15–50)",
        queueLong = "நீண்ட வரிசை (50+)",
        verifiedDealer = "சரிபார்க்கப்பட்ட முகவர்",
        unverifiedDealer = "சரிபார்ப்பில் உள்ளது",
        liveCylinderStock = "நேரடி சிலிண்டர் இருப்பு",
        queueTelemetry = "வரிசை மற்றும் மக்கள் உறுதிப்படுத்தல்",
        peopleInQueue = "பேர் வரிசையில்",
        estWaitMins = "நிமிட காத்திருப்பு",
        trustScore = "நம்பிக்கை",
        watchRestock = "இருப்பு வந்தால் அறிவி",
        watchingRestock = "அறிவிப்பு செயலில்",
        directions = "வழித்தடம்",
        callDealer = "அழைக்க",
        submitQueueReport = "வரிசை / இருப்பு தகவல் அனுப்பு",
        confirmStockAccurate = "தகவலைப் பதிவு செய்",
        sellerDashboardTitle = "முகவர் இருப்பு கட்டுப்பாடு",
        registerNewOutlet = "புதிய நிலையம்",
        publishStockUpdate = "நேரடி இருப்பைப் புதுப்பி",
        markSoldOutNow = "விற்றுவிட்டதாகக் குறிக்கவும்",
        adminConsoleTitle = "தேசிய விநியோக மற்றும் நிர்வாகப் பலகை",
        crowdDiscrepancyWarning = "மக்கள் எச்சரிக்கை: அருகிலுள்ள பல பயனர்கள் இருப்பு முடிந்துவிட்டதாகத் தெரிவித்துள்ளனர்."
    )

    fun forLanguage(language: AppLanguage): TrilingualStrings = when (language) {
        AppLanguage.EN -> english
        AppLanguage.SI -> sinhala
        AppLanguage.TA -> tamil
    }
}

val LocalStrings = staticCompositionLocalOf { LankaGasTranslations.english }
