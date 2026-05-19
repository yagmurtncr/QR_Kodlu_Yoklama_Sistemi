# 🎓 Smart QR: Dinamik Yoklama ve Devam Takip Sistemi

[![Kotlin](https://img.shields.io/badge/Language-Kotlin-orange.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-yellow.svg)](https://firebase.google.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Geleneksel yoklama yöntemlerindeki zaman kaybını ve sahteciliği (arkadaş yerine imza atma) ortadan kaldıran, **bulut tabanlı, gerçek zamanlı ve yüksek güvenlikli** bir mobil eğitim yönetim platformudur.

---

## 📸 Görsel Önizleme
| Karşılama Ekranı | Öğretmen Paneli | Öğrenci Dashboard |
|:---:|:---:|:---:|
| <img src="https://raw.githubusercontent.com/username/repo/main/screenshots/auth.png" width="200"> | <img src="https://raw.githubusercontent.com/username/repo/main/screenshots/teacher.png" width="200"> | <img src="https://raw.githubusercontent.com/username/repo/main/screenshots/student.png" width="200"> |

---

## 🚀 Öne Çıkan Özellikler

### 🛡️ Güvenlik ve İnovasyon
*   **Dinamik Tokenizasyon:** QR kod statik değildir. `UUID` ile her 20 saniyede bir değişir. Fotoğraf çekip paylaşmak geçersizdir.
*   **GPS Geofencing (Konum Doğrulama):** Öğrencinin yoklama verebilmesi için öğretmen ile aynı koordinatta (100m yarıçapında) olması şarttır.
*   **E-posta Doğrulama:** Laboratuvar isterlerine uygun olarak, mail onayı yapmayan kullanıcılar sisteme giriş yapamaz.
*   **Web QR (Projeksiyon Desteği):** Öğretmen tek tıkla QR kodu sınıfın projeksiyon ekranına yansıtabilir.

### 📊 Raporlama ve Analiz
*   **Akıllı Dashboard:** `MPAndroidChart` ile katılım oranlarını pasta ve sütun grafikleriyle anlık takip edin.
*   **Resmi PDF Raporu:** `iText7` motoru ile tek tıkla profesyonel yoklama listesi oluşturun.
*   **Excel (XLSX) Dışa Aktarma:** `Apache POI` entegrasyonu ile verileri üniversite otomasyon sistemlerine uyumlu formatta indirin.

### 🏛️ Kurumsal Hiyerarşi
*   **Admin:** Üniversite/Kurum tanımlama ve ders-hoca atamalarını yönetme.
*   **Öğretmen:** Ders başlatma, dinamik QR yönetimi ve rapor alma.
*   **Öğrenci:** Atandığı derslere katılım sağlama ve kendi istatistiklerini izleme.

---

## 🛠️ Teknoloji Yığını (Tech Stack)

*   **Dil:** Kotlin
*   **Mimari:** MVVM, View Binding
*   **Backend:** Firebase (Auth, Firestore, Cloud Messaging)
*   **Görüntü İşleme:** Google ML Kit (Barcode Scanning) & CameraX
*   **Analiz:** MPAndroidChart
*   **Raporlama:** iText7 (PDF) & Apache POI (Excel)
*   **UI/UX:** Lottie Animations & Modern Material 3 (SaaS Style)

---

## 📦 Kurulum

1.  Projeyi klonlayın:
    ```bash
    git clone https://github.com/ytunc/QR_Kodlu_Yoklama_Sistemi.git
    ```
2.  `google-services.json` dosyasını Firebase Console'dan indirip `app/` klasörüne kopyalayın.
3.  Android Studio'da projeyi açın ve **Sync Gradle** yapın.
4.  Gerekli İzinler: Uygulama ilk açılışta **Kamera** ve **Konum** izni isteyecektir.

---

## 📐 Sistem Akışı (Activity Diagram)

1.  **Giriş:** Kullanıcı rolü (Admin/Hoca/Öğrenci) otomatik tanınır.
2.  **Öğretmen:** Ders seçilir -> Konum kaydedilir -> 20 saniyelik QR döngüsü başlar.
3.  **Öğrenci:** QR okutulur -> Token kontrolü yapılır -> Konum (GPS) doğrulanır.
4.  **Sonuç:** Veriler Firestore'a işlenir ve grafikler anlık güncellenir.

---

## 🌐 Web QR Sayfası

Öğretmen panelindeki **Bilgisayarda Göster (Web QR)** butonu artık bir Firebase Hosting adresi açar. Sayfa `Lessons/{lessonId}` dokümanını dinler ve `activeToken` değiştikçe QR kodu canlı günceller.

### Deploy

1. Firebase CLI kurun.
2. Proje kökünde oturum açın ve hosting başlatın:

```bash
firebase login
firebase init hosting
firebase deploy --only hosting
```

3. Deploy sonrası adres şu biçimde olur:

```text
https://qr-kodlu-yoklama-takip-sistemi.web.app/?lessonId=<LESSON_ID>
```

`<LESSON_ID>` yerine öğretmen ekranındaki ders ID'si gelir. Öğretmen bu sayfayı bilgisayarda açtığında QR kod otomatik görünür ve 20 saniyede bir yenilenir.

---

## 📧 İletişim & Geliştirici
**Yağmur Tuncer** - [GitHub Profiliniz](https://github.com/ytunc)

---
*Bu proje, modern eğitim teknolojilerinde güvenliği ve hızı birleştirmek amacıyla geliştirilmiştir.*
