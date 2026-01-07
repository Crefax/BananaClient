package com.muzmod.navigation;

/**
 * Navigation Sistemi Kullanım Örneği
 * =================================
 * 
 * Bu dosya sadece dokümantasyon amaçlıdır.
 * MiningState veya diğer state'lerde kullanım örneği:
 * 
 * ```java
 * import com.muzmod.navigation.NavigationManager;
 * import com.muzmod.navigation.Direction;
 * 
 * // NavigationManager'ı al
 * NavigationManager nav = NavigationManager.getInstance();
 * 
 * // === TEMEL HAREKETLER ===
 * 
 * // 50 blok ileri git
 * nav.goForward(50);
 * 
 * // 30-60 blok arası rastgele mesafe ileri git
 * nav.goForward(30, 60);
 * 
 * // Belirli koordinata git (pathfinding ile)
 * nav.goTo(new BlockPos(100, 64, 200));
 * 
 * // Yöne doğru git
 * nav.goDirection(Direction.EAST, 20);        // Doğuya 20 blok
 * nav.goDirection(Direction.NORTH, 10, 15);   // Kuzeye 10-15 blok
 * nav.goDirection(Direction.LEFT, 5);         // Sola 5 blok
 * nav.goDirection(Direction.FORWARD, 30, 50); // İleri 30-50 blok
 * 
 * // === DÖNÜŞLER ===
 * 
 * nav.turnLeft(90);    // 90 derece sola dön
 * nav.turnRight(45);   // 45 derece sağa dön
 * nav.turnAround();    // 180 derece dön (arkaya bak)
 * nav.rotateTo(-90);   // Belirli yaw açısına dön (East)
 * nav.lookAt(blockPos); // Bir bloğa doğru bak
 * 
 * // === ZİNCİRLEME (Chaining) ===
 * 
 * nav.goForward(30, 50)
 *    .onComplete(() -> {
 *        // İleri gitme tamamlandığında
 *        System.out.println("Hedefe ulaşıldı!");
 *    })
 *    .onFailed(() -> {
 *        // Başarısız olduğunda
 *        System.out.println("Yol bulunamadı!");
 *    });
 * 
 * // Dönüş + hareket kombinasyonu
 * nav.turnLeft(90)
 *    .goForward(20);
 * 
 * // === DURUM KONTROLÜ ===
 * 
 * // Hareket ediyor mu?
 * if (nav.isNavigating()) {
 *     // Hareket halinde
 * }
 * 
 * // Dönüyor mu?
 * if (nav.isRotating()) {
 *     // Dönüş yapıyor
 * }
 * 
 * // Meşgul mü? (hareket veya dönüş)
 * if (nav.isBusy()) {
 *     return; // Bekle
 * }
 * 
 * // Hedefe olan mesafe
 * double distance = nav.getDistanceToTarget();
 * 
 * // Durum metni
 * String status = nav.getStatus(); // "Going to 100,64,200 (45.3 blocks)"
 * 
 * // === DURDURMA ===
 * 
 * nav.stop(); // Tüm hareketleri durdur
 * 
 * // === RENDER ===
 * 
 * // Yol render'ını aç/kapa
 * nav.getRenderer().setRenderEnabled(true);
 * 
 * // Çizgi kalınlığını ayarla
 * nav.getRenderer().setLineWidth(3.0f);
 * 
 * // === MiningState'de Kullanım Örneği ===
 * 
 * private void handleInitialWalk() {
 *     NavigationManager nav = NavigationManager.getInstance();
 *     
 *     if (!nav.isNavigating()) {
 *         // Henüz başlamadıysa, yürüyüşü başlat
 *         nav.goForward(30, 60)
 *            .setRotationSpeed(20)
 *            .onComplete(() -> {
 *                // Yürüyüş tamamlandı, sonraki phase'e geç
 *                setPhase(MiningPhase.FINDING_ORE);
 *            })
 *            .onFailed(() -> {
 *                // Yürüyüş başarısız, yeniden dene
 *                MuzMod.LOGGER.warn("Walk failed, retrying...");
 *            });
 *         return;
 *     }
 *     
 *     // Yürürken kazma da yapabilirsin
 *     if (nav.isNavigating()) {
 *         // Yol üzerinde cevher ara ve kaz
 *         BlockPos ore = findNearbyOre();
 *         if (ore != null) {
 *             mineBlock(ore);
 *         }
 *     }
 * }
 * 
 * // === Pathfinding Özellikleri ===
 * 
 * // Pathfinder'a direkt erişim
 * PathFinder pathFinder = new PathFinder();
 * 
 * // Yol bul
 * List<BlockPos> path = pathFinder.findPath(start, end);
 * 
 * // Direkt yol açık mı?
 * boolean clear = pathFinder.isDirectPathClear(start, end);
 * 
 * // Pozisyon yürünebilir mi?
 * boolean walkable = pathFinder.isWalkable(pos);
 * ```
 * 
 * === GÖRSEL ===
 * 
 * Render sistemi otomatik olarak:
 * - Hedef bloğu KIRMIZI kutu olarak gösterir
 * - Yolu YEŞİL çizgi olarak çizer
 * - Waypoint'leri SARI kutular olarak gösterir
 * - Mevcut waypoint'i MAVİ gösterir
 * 
 * Bu sistem, tüm state'ler arasında paylaşımlı kullanılır.
 * WorldRenderer otomatik olarak render işlemini yapar.
 */
public class NavigationExample {
    // Bu sınıf sadece dokümantasyon amaçlıdır.
    // Gerçek implementasyonda kullanılmaz.
}
