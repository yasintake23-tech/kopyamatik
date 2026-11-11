# Kopyamatik - AI Soru Çözme & Kesintisiz Seslendirme Uygulaması

Android (Kotlin & Jetpack Compose) tabanlı, görseldeki soruları Gemini 1.5 Flash Vision ile çözen ve Fish Audio TTS ile parçalayarak birleştirip arka planda kesintisiz seslendiren mobil uygulama.

---

## 🚀 Temel Özellikler

1. **Minimalist & Bembeyaz Tasarım:**
   - Ekran tamamen bembeyaz (`#FFFFFF`).
   - Sağ üst köşede sade Ayarlar dişli çark butonu.
2. **Ayarlar ve Kalıcı Anahtar Yönetimi:**
   - `Gemini API Key`, `Fish Audio API Key`, `Fish Audio Voice Model ID (reference_id)` alanları.
   - **"Kontrol Et ve Kaydet":** API'leri ufak bir istek ile doğrular, geçerliyse Android Jetpack DataStore içerisine kalıcı olarak saklar.
3. **AI Soru Çözüm Motoru (Gemini 1.5 Flash Vision):**
   - Kamera veya Galeriden seçilen fotoğrafları Base64 olarak Gemini Vision API'sine aktarır.
   - Soruları adım adım, cevapları 6 kelimelik gruplar halinde 6'şar kez tekrar eden özel seslendirme sistem istemine uygun çözer.
   - Markdown (`*`, `#` vb.) karakterlerini otomatik temizleyerek pürüzsüz metin üretir.
4. **Fish Audio TTS (500 Karakterlik Parçalama & Birleştirme):**
   - Üretilen metni kelimeleri bölmeden en fazla 500 karakterlik parçalara (`chunk`) ayırır.
   - Her parçayı sırayla `https://api.fish.audio/v1/tts` adresine gönderir.
   - Dönen MP3 bayt akışlarını sıralı tek bir yerel ses dosyasında birleştirir.
5. **Media3 ExoPlayer & MediaSessionService (Kesintisiz Arka Plan Oynatma):**
   - `FOREGROUND_SERVICE_MEDIA_PLAYBACK` ve `WAKE_LOCK` entegrasyonu.
   - Ekran kapansa veya uygulama alta alınsa dahi ses çalmaya devam eder.
   - Bluetooth ve kablolu kulaklık tuşlarıyla (durdur/başlat) tam uyumluluk.
   - Kulaklık çıkarıldığında otomatik duraklatma (`setHandleAudioBecomingNoisy(true)`).
   - **Dinamik Hız Kontrolü:** `0.5x`, `0.75x`, `1.0x`, `1.25x`, `1.5x`, `2.0x` hız seçenekleri anında uygulanır.

---

## 📁 Proje Dizin Yapısı

```
kopyamatik/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/kopyamatik/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── KopyamatikApp.kt
│   │   │   ├── data/
│   │   │   │   ├── SettingsDataStore.kt   # API Anahtarlarını kalıcı saklama
│   │   │   │   ├── TextChunker.kt         # 500 karakter kelime korumalı bölücü
│   │   │   │   ├── GeminiService.kt       # Soru çözme & markdown temizleme
│   │   │   │   └── FishAudioService.kt    # TTS parçalama & MP3 birleştirme
│   │   │   ├── service/
│   │   │   │   └── AudioPlaybackService.kt# Media3 Foreground Playback & Kulaklık Kontrolü
│   │   │   └── ui/
│   │   │       ├── HomeScreen.kt          # Bembeyaz UI, Kamera/SS, Oynatıcı & Hız Kontrolü
│   │   │       ├── SettingsScreen.kt      # API anahtarları test & kayıt ekranı
│   │   │       ├── MainViewModel.kt       # Pipeline ve oynatıcı durum yönetimi
│   │   │       └── theme/
│   │   │           ├── Color.kt
│   │   │           ├── Theme.kt
│   │   │           └── Type.kt
│   │   └── res/
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 🛠️ Nasıl Çalıştırılır?

1. Bu projeyi **Android Studio Ladybug (veya daha yeni bir sürüm)** ile açın.
2. Gradle senkronizasyonunun tamamlanmasını bekleyin.
3. Fiziksel bir Android cihaza veya Android Emülatörüne yükleyin (`Run 'app'`).
4. İlk açılışta sağ üstteki dişli simgesine basarak API anahtarlarınızı girin ve **"Kontrol Et ve Kaydet"** butonuna basın.
5. Ana ekrana dönüp **"SS Yükle"** veya **"Fotoğraf Çek"** diyerek sorularınızı yükleyin ve **"Soruları Çöz ve Seslendir"** butonuna basın.
