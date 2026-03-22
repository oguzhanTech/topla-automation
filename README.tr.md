# Kullanım özeti (Türkçe)

Önce `.env.example` dosyasını `.env` olarak kopyalayın ve **en az** şunları doldurun: `TOPLA_API_BASE_URL`, `TOPLA_IMPORT_API_KEY`, `TOPLA_ACTOR_KEY` (deal-radar tarafında tanımlı actor ile aynı olmalı).

Derleme ve çalıştırma:

```bash
mvn clean package
java -jar target/topla-deal-ingestion-1.0.0-SNAPSHOT.jar
```

Aşağıda üç basit senaryo var. **Jar çalışırken `.feature` dosyası okunmaz**; Cucumber’daki adet örnekleri yalnızca test / sözleşme içindir. Hub’da kaç deal istediğinizi **`.env` içinde** `AMAZON_DEALS_TARGET_COUNT` ile verirsiniz.

---

## Senaryo 1 — Amazon: Bildiğiniz ürün sayfaları (sabit link)

Belirli ürün URL’lerinden her biri için bir deal üretir.

**`.env` örneği:**

```env
INGESTION_SOURCES=amazon
AMAZON_MODE=urls
AMAZON_URLS=https://www.amazon.com.tr/dp/URUN1,https://www.amazon.com.tr/dp/URUN2
```

Tek sayfa için `AMAZON_URLS` yerine sadece `AMAZON_START_URL` de yeterli.

---

## Senaryo 2 — Amazon: Fırsat listesi (hub), rastgele N adet

Fırsat / indirim sayfasına gider, indirimli görünen ürünlerden bir havuz oluşturur, karıştırır, en fazla **N** tanesinin detayını çekip import eder.

**`.env` örneği:**

```env
INGESTION_SOURCES=amazon
AMAZON_MODE=hub
AMAZON_DEALS_HUB_URL=https://www.amazon.com.tr/deals
AMAZON_DEALS_TARGET_COUNT=5
# İsteğe bağlı: aynı sırayı tekrarlamak için
# AMAZON_DEALS_RANDOM_SEED=42
```

Aynı koşuda aynı ürün iki kez import edilmesin diye uygulama içi **tekrar kontrolü** vardır; veritabanında daha önce kayıtlı deal’ları kontrol etmez.

---

## Senaryo 3 — İki kaynak: Amazon + Trendyol (demo)

Her kaynak sırayla çalışır; Trendyol tarafı şu an **demo/placeholder** veridir.

**`.env` örneği:**

```env
INGESTION_SOURCES=amazon,trendyol
AMAZON_MODE=urls
AMAZON_START_URL=https://www.amazon.com.tr/
```

---

## Sadece testler (Cucumber)

```bash
mvn test
```

`src/test/resources/features/` altındaki senaryolar doğrulama ve hub adet sözleşmesi içindir; canlı tarayıcı + gerçek import **varsayılan testte** çalışmaz.
