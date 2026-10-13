# Nguồn ảnh và thư viện bên thứ ba

Đồ án học tập, phi thương mại. Ảnh minh hoạ sản phẩm là **ảnh mẫu** lấy từ Wikimedia Commons (đã thu nhỏ về tối đa 800px, nén JPEG),
dùng đúng giấy phép của từng ảnh. Ảnh minh hoạ cho đúng loại hàng chứ không phải đúng thương hiệu/mẫu mã của sản phẩm trong cửa hàng.
Khi thay bằng ảnh thật của shop, Admin chỉ cần tải ảnh mới ở trang Quản lý sản phẩm (ảnh mẫu không bị xoá khỏi mã nguồn).

## Ảnh sản phẩm (`backend/src/main/webapp/static/img/products/`)

| Sản phẩm | File | Tên file gốc | Tác giả | Giấy phép | Nguồn |
|---|---|---|---|---|---|
| Hạt giống cà chua Beef F1 | `static/img/products/hat-giong-ca-chua.jpg` | Tomato 01.jpg | Brett Taylor from Wellington, New Zealand | CC BY 2.0 | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Tomato_01.jpg) |
| Hạt giống rau cải xanh | `static/img/products/hat-giong-cai-xanh.jpg` | Brassica rapa chinensis 4610994.jpg | Jon Sullivan | CC BY 4.0 | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Brassica_rapa_chinensis_4610994.jpg) |
| Phân bón NPK 20-20-15 | `static/img/products/phan-bon-npk.jpg` | NPK 19-19-19 Fertilizer in 1 KG Commercial Packing (1).jpg | Suyash Dwivedi | CC BY-SA 4.0 | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:NPK_19-19-19_Fertilizer_in_1_KG_Commercial_Packing_(1).jpg) |
| Phân hữu cơ vi sinh | `static/img/products/phan-huu-co-vi-sinh.jpg` | 20kg Fertilizer.jpg | Azorbli | CC BY-SA 4.0 | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:20kg_Fertilizer.jpg) |
| Máy phun thuốc đeo vai | `static/img/products/may-phun-thuoc.jpg` | Knapsack sprayer.jpg | Safapalanadan94 | CC BY-SA 4.0 | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Knapsack_sprayer.jpg) |
| Bộ tưới nhỏ giọt 50m | `static/img/products/bo-tuoi-nho-giot.jpg` | Button dripper.JPG | Borisshin | CC BY-SA 4.0 | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Button_dripper.JPG) |
| Cuốc chim thép cán gỗ | `static/img/products/cuoc-chim.jpg` | Pickaxe.jpg | Hyena~commonswiki | Public domain | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Pickaxe.jpg) |
| Máy cắt cỏ cầm tay | `static/img/products/may-cat-co.jpg` | Brush cutter 02.JPG | Feťour | Public domain | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Brush_cutter_02.JPG) |
| Thuốc trừ sâu Abamectin | `static/img/products/thuoc-tru-sau.jpg` | Insecticide Spray.jpg | Daniel Abugre Anyorigya | CC BY-SA 4.0 | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Insecticide_Spray.jpg) |
| Kéo cắt tỉa cành | `static/img/products/keo-cat-tia.jpg` | Pruning shears.jpg | barockschloss from Zeilitzheim, Germany | CC BY 2.0 | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Pruning_shears.jpg) |
| Máy xới đất mini | `static/img/products/may-xoi-dat.jpg` | Rotary tiller compact.jpg | Green | CC BY-SA 3.0 | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Rotary_tiller_compact.jpg) |
| Gạo hữu cơ ST25 | `static/img/products/gao-st25.jpg` | Rice Grains .png | Hpannu7 | CC BY-SA 4.0 | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Rice_Grains_.png) |

CC BY / CC BY-SA yêu cầu ghi công tác giả (bảng trên) và nêu giấy phép; ảnh CC BY-SA nếu chỉnh sửa/phát hành lại phải giữ cùng giấy phép.

## Icon

[Lucide](https://lucide.dev) v0.469.0, giấy phép ISC. Nạp từ CDN jsDelivr (`https://cdn.jsdelivr.net/npm/lucide@0.469.0/dist/umd/lucide.min.js`) trong `common/head.jspf`, không lưu trong repo.
Cách dùng: `<i data-lucide="ten-icon" class="size-5"></i>` rồi `static/js/icons.js` đổi thành SVG (và vẽ lại sau mỗi lần htmx thay nội dung). Tìm tên icon tại https://lucide.dev/icons.

## Ảnh sản phẩm bổ sung (`DataSeeder.MORE_PRODUCTS`, 70 ảnh)

Cùng thư mục `static/img/products/`, cùng nguồn Wikimedia Commons, thu nhỏ tối đa 640px. Ảnh chỉ minh hoạ đúng loại hàng.

| Sản phẩm | File | Tên file gốc | Tác giả | Giấy phép | Nguồn |
|---|---|---|---|---|---|
| Hạt giống dưa leo F1 (gói 20 hạt) | `hat-giong-dua-leo.jpg` | Cucumber plants 4.jpg | Pascal Kings | CC BY 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Cucumber_plants_4.jpg) |
| Hạt giống ớt hiểm (gói 30 hạt) | `hat-giong-ot-hiem.jpg` | "Aji dulce" - Ají Amazónico.jpg | SaskyaCervera | CC BY 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:%22Aji_dulce%22_-_Aj%C3%AD_Amaz%C3%B3nico.jpg) |
| Hạt giống rau muống (gói 100g) | `hat-giong-rau-muong.jpg` | Adat Ombathumuri Paadam Thrissur DSCN1491.JPG | Manojk | CC BY 3.0 | [Commons](https://commons.wikimedia.org/wiki/File:Adat_Ombathumuri_Paadam_Thrissur_DSCN1491.JPG) |
| Hạt giống xà lách lô lô xanh (gói 5g) | `hat-giong-xa-lach.jpg` | Główka sałaty - 2025.07.04.jpg | Aw58 | CC BY 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:G%C5%82%C3%B3wka_sa%C5%82aty_-_2025.07.04.jpg) |
| Hạt giống bí đỏ hồ lô (gói 10 hạt) | `hat-giong-bi-do.jpg` | Andrew Branch 2016-10-01 (Unsplash XJRDzDZFgqU).jpg | Andrew Branch denae_andrew | CC0 | [Commons](https://commons.wikimedia.org/wiki/File:Andrew_Branch_2016-10-01_(Unsplash_XJRDzDZFgqU).jpg) |
| Hạt giống đậu cove leo (gói 50g) | `hat-giong-dau-cove.jpg` | Green beans in container.jpg | Geo Lightspeed7 | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Green_beans_in_container.jpg) |
| Hạt giống cà tím dài (gói 30 hạt) | `hat-giong-ca-tim.jpg` | -2017-06-21 Aubergine, Trimingham.JPG | 



Kolforn (Kolforn)
I'd appreciate if you could mail me (Kolforn@gma | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:-2017-06-21_Aubergine,_Trimingham.JPG) |
| Hạt giống hoa hướng dương (gói 50 hạt) | `hat-giong-huong-duong.jpg` | Arkaia - Girasoles -BT- 01.jpg | Basotxerri | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Arkaia_-_Girasoles_-BT-_01.jpg) |
| Hạt giống dưa hấu không hạt (gói 10 hạt) | `hat-giong-dua-hau.jpg` | Citrullus lanatus (Thunb.) Cucurbitaceae (7174523558).jpg | Len Worthington | CC BY-SA 2.0 | [Commons](https://commons.wikimedia.org/wiki/File:Citrullus_lanatus_(Thunb.)_Cucurbitaceae_(7174523558).jpg) |
| Hạt giống bắp ngọt Mỹ (gói 100g) | `hat-giong-bap-ngot.jpg` | Barbe de maïs.jpg | Sidoine Mbogni | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Barbe_de_ma%C3%AFs.jpg) |
| Hạt giống mướp hương (gói 20 hạt) | `hat-giong-muop.jpg` | Luffa sponges spelled loofah.jpg | SarKaLay စာကလေး | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Luffa_sponges_spelled_loofah.jpg) |
| Hạt giống rau mùi (ngò rí) (gói 50g) | `hat-giong-ngo-ri.jpg` | Beautiful Coriander plant Flowers.jpg | Kuldeep Singh Deep | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Beautiful_Coriander_plant_Flowers.jpg) |
| Phân đạm Urê (bao 50kg) | `phan-ure.jpg` | CH4N2O crystal P9131034.jpg | あおもりくま | CC BY-SA 3.0 | [Commons](https://commons.wikimedia.org/wiki/File:CH4N2O_crystal_P9131034.jpg) |
| Phân lân nung chảy (bao 25kg) | `phan-lan.jpg` | Tripelsuperfosfaat (triple phosphate).jpg | Rasbak | CC BY-SA 3.0 | [Commons](https://commons.wikimedia.org/wiki/File:Tripelsuperfosfaat_(triple_phosphate).jpg) |
| Phân Kali Clorua – KCl (bao 25kg) | `phan-kali.jpg` | Compacted potassium chloride, fertilizer grade.jpg | Abbas cucaniensis | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Compacted_potassium_chloride,_fertilizer_grade.jpg) |
| Phân DAP (bao 50kg) | `phan-dap.jpg` | DAP (Di-Ammonium Phosphate) in a 1 kg commercial package (2).jpg | Suyash Dwivedi | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:DAP_(Di-Ammonium_Phosphate)_in_a_1_kg_commercial_package_(2).jpg) |
| Phân bón lá Growmore (gói 500g) | `phan-bon-la.jpg` | Crop Spraying near Saxby All Saints - geograph.org.uk - 1762793.jpg | David Wright | CC BY-SA 2.0 | [Commons](https://commons.wikimedia.org/wiki/File:Crop_Spraying_near_Saxby_All_Saints_-_geograph.org.uk_-_1762793.jpg) |
| Phân trùn quế (bao 5kg) | `phan-trun-que.jpg` | Lombricompostage - vermicomposting (6146469849).jpg | SuSanA Secretariat | CC BY 2.0 | [Commons](https://commons.wikimedia.org/wiki/File:Lombricompostage_-_vermicomposting_(6146469849).jpg) |
| Phân gà hữu cơ vi sinh (bao 20kg) | `phan-ga.jpg` | Manure (yetti).jpg | Paul Dixon, Swadlincote, England | CC BY-SA 2.0 | [Commons](https://commons.wikimedia.org/wiki/File:Manure_(yetti).jpg) |
| Vôi bột cải tạo đất (bao 25kg) | `voi-bot.jpg` | Calcium oxide powder.JPG | Leiem | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Calcium_oxide_powder.JPG) |
| Phân bón NPK 16-16-8 (bao 25kg) | `npk-16-16-8.jpg` | Kalimagnesia.jpg | Dr. Eugen Lehle, http://bodenlabor.de | CC BY-SA 3.0 | [Commons](https://commons.wikimedia.org/wiki/File:Kalimagnesia.jpg) |
| Đạm cá hữu cơ (chai 1 lít) | `dam-ca.jpg` | Lower Fish Creek 024 (5549976945).jpg | Pacific Southwest Region 5 | Public domain | [Commons](https://commons.wikimedia.org/wiki/File:Lower_Fish_Creek_024_(5549976945).jpg) |
| Mùn dừa xử lý (bao 10kg) | `mun-dua.jpg` | Coco peat.jpg | Lawrence756 | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Coco_peat.jpg) |
| Tro trấu hun (bao 15kg) | `tro-tau.jpg` | Rice husk.jpg | GST HBK | Public domain | [Commons](https://commons.wikimedia.org/wiki/File:Rice_husk.jpg) |
| Thuốc diệt cỏ Glyphosate 480 (chai 1 lít) | `thuoc-diet-co.jpg` | 20240723 army corps of engineers hydrilla infestation keeney cove PD207528.jpg | Paul Danese | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:20240723_army_corps_of_engineers_hydrilla_infestation_keeney_cove_PD207528.jpg) |
| Thuốc trừ nấm Mancozeb 80WP (gói 500g) | `thuoc-tru-nam.jpg` | Dequadin Oral Paint2.jpg | ? | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Dequadin_Oral_Paint2.jpg) |
| Thuốc trừ rầy Imidacloprid (chai 100ml) | `thuoc-tru-ray.jpg` | Bird Cherry-oat Aphid.jpg | Jaiiiiiiii | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Bird_Cherry-oat_Aphid.jpg) |
| Bẫy dính côn trùng màu vàng (xấp 20 tấm) | `bay-dinh-vang.jpg` | Citrus Sampling (20089222-APHIS-DB-0018).jpg | USDAgov | Public domain | [Commons](https://commons.wikimedia.org/wiki/File:Citrus_Sampling_(20089222-APHIS-DB-0018).jpg) |
| Thuốc diệt ốc bươu vàng (gói 1kg) | `thuoc-oc-buou.jpg` | F7951CacarongMatandafvf 13.JPG | Judgefloro | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:F7951CacarongMatandafvf_13.JPG) |
| Chế phẩm sinh học Trichoderma (gói 1kg) | `trichoderma.jpg` | Conidial mass of Trichoderma spp. under 400x magnification.jpg | Arpitoon | CC0 | [Commons](https://commons.wikimedia.org/wiki/File:Conidial_mass_of_Trichoderma_spp._under_400x_magnification.jpg) |
| Dầu neem xua đuổi sâu (chai 500ml) | `dau-neem.jpg` | "Neem flower for rasam making".jpg | Thamizhpparithi Maari | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:%22Neem_flower_for_rasam_making%22.jpg) |
| Bình xịt tay 2 lít | `binh-xit-tay.jpg` | Gartenspritze.jpg | User:MarkusHagenlocher | CC BY-SA 3.0 | [Commons](https://commons.wikimedia.org/wiki/File:Gartenspritze.jpg) |
| Mồi sinh học diệt chuột (hộp 200g) | `thuoc-diet-chuot.jpg` | Long-tailed black rat (Rattus rattus).jpg | Alex O'Neal | CC BY-SA 2.0 | [Commons](https://commons.wikimedia.org/wiki/File:Long-tailed_black_rat_(Rattus_rattus).jpg) |
| Thuốc trừ sâu tơ Emamectin (gói 10g) | `thuoc-sau-to.jpg` | Diamond-back moth (NH266) (29267043193).jpg | David Short from Windsor, UK | CC BY 2.0 | [Commons](https://commons.wikimedia.org/wiki/File:Diamond-back_moth_(NH266)_(29267043193).jpg) |
| Xẻng đào đất cán dài | `xeng.jpg` | -2023-01-23 Garden Spade and Fork, Trimingham, Norfolk.JPG | 



Kolforn (Kolforn)
I'd appreciate if you could mail me (Kolforn@gma | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:-2023-01-23_Garden_Spade_and_Fork,_Trimingham,_Norfolk.JPG) |
| Cào cỏ 14 răng | `cao-co.jpg` | Molln Wilderer Museum Schaufeln und Rechen.jpg | Geolina163 | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Molln_Wilderer_Museum_Schaufeln_und_Rechen.jpg) |
| Bình tưới cây 5 lít | `binh-tuoi.jpg` | Bucket - Flickr - anamobe.jpg | Ana Bernardo from London, United Kindgdom | CC BY-SA 2.0 | [Commons](https://commons.wikimedia.org/wiki/File:Bucket_-_Flickr_-_anamobe.jpg) |
| Găng tay làm vườn (đôi) | `gang-tay.jpg` | Gloves DSC 1984.jpg | el cajon yacht club | CC BY 2.0 | [Commons](https://commons.wikimedia.org/wiki/File:Gloves_DSC_1984.jpg) |
| Liềm cắt lúa lưỡi răng cưa | `liem.jpg` | A sickle, reaping hook.jpg | Thamizhpparithi Maari | CC BY-SA 3.0 | [Commons](https://commons.wikimedia.org/wiki/File:A_sickle,_reaping_hook.jpg) |
| Dao phát bờ cán gỗ | `dao-phat.jpg` | Kompletní souprava Skautského kuchaře 31.jpg | Pohled 111 | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Kompletn%C3%AD_souprava_Skautsk%C3%A9ho_kucha%C5%99e_31.jpg) |
| Cuốc xoi 2 mũi | `cuoc-xoi.jpg` | Haue im Kilianstollen Marsberg.jpg | © 1971markus (☠): ⇒ Laberkasten ... | CC BY-SA 3.0 de | [Commons](https://commons.wikimedia.org/wiki/File:Haue_im_Kilianstollen_Marsberg.jpg) |
| Xe rùa chở đất 1 bánh | `xe-rua.jpg` | 20130613 Budapest 04.jpg | Mark Ahsmann | CC BY-SA 3.0 | [Commons](https://commons.wikimedia.org/wiki/File:20130613_Budapest_04.jpg) |
| Cào xới 3 chấu cầm tay | `xoi-3-chau.jpg` | TrowelPS.jpg | Przemysław Sakrajda | CC BY-SA 3.0 | [Commons](https://commons.wikimedia.org/wiki/File:TrowelPS.jpg) |
| Cưa cắt cành lưỡi cong | `cua-cat-canh.jpg` | Astsägen.jpg | StromBer | CC BY-SA 2.0 de | [Commons](https://commons.wikimedia.org/wiki/File:Asts%C3%A4gen.jpg) |
| Giỏ nhựa đựng nông sản | `gio-thu-hoach.jpg` | Berbagai jenis produk anyaman.jpg | Wiwil13 | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Berbagai_jenis_produk_anyaman.jpg) |
| Nón lá chống nắng | `non-la.jpg` | 120505-A-ON889-019 (7145990079).jpg | U.S. Army Corps of Engineers Norfolk District from United States | Public domain | [Commons](https://commons.wikimedia.org/wiki/File:120505-A-ON889-019_(7145990079).jpg) |
| Máy bơm nước 1HP | `may-bom-nuoc.jpg` | Agricultural irrigation pump set with borewell, Vrindavan, Uttar Pradesh, India.jpg | Gannu03 | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Agricultural_irrigation_pump_set_with_borewell,_Vrindavan,_Uttar_Pradesh,_India.jpg) |
| Đầu phun mưa xoay (bộ 10 cái) | `dau-phun-mua.jpg` | Komoča Beach 52.jpg | Vauia Rex | CC0 | [Commons](https://commons.wikimedia.org/wiki/File:Komo%C4%8Da_Beach_52.jpg) |
| Ống tưới phun mưa PE (cuộn 100m) | `ong-tuoi.jpg` | Avarai.jpg | Nithin Abi | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Avarai.jpg) |
| Máy tuốt lúa mini | `may-tuot-lua.jpg` | 078 MNACTEC (Terrassa), exposició Enérgeia, màquina de batre d'Ajuria.jpg | Enric | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:078_MNACTEC_(Terrassa),_exposici%C3%B3_En%C3%A9rgeia,_m%C3%A0quina_de_batre_d%27Ajuria.jpg) |
| Máy xay xát gạo mini | `may-xay-xat.jpg` | Rice mill in Laos.jpg | Basile Morin | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Rice_mill_in_Laos.jpg) |
| Bồn chứa nước nhựa 1000 lít | `bon-nuoc.jpg` | -55 Watertank.jpg | Park taeho | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:-55_Watertank.jpg) |
| Máy cày mini 9HP | `may-cay-mini.jpg` | Motocultivadora para uso en pequeñas parcelas en pendiente.JPG | Fev | Public domain | [Commons](https://commons.wikimedia.org/wiki/File:Motocultivadora_para_uso_en_peque%C3%B1as_parcelas_en_pendiente.JPG) |
| Máy làm cỏ cầm tay chạy xăng | `may-lam-co.jpg` | 10-OL 102302A.jpg | T442163 | CC BY-SA 3.0 | [Commons](https://commons.wikimedia.org/wiki/File:10-OL_102302A.jpg) |
| Máy gieo hạt đẩy tay | `may-gieo-hat.jpg` | Agricultura.ping.jpg | Alonso Carreon Soto | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Agricultura.ping.jpg) |
| Máy bơm tăng áp mini | `may-bom-tang-ap.jpg` | Russian booster pump IMG 0417.JPG | Guido Zsilavacz | CC BY-SA 3.0 | [Commons](https://commons.wikimedia.org/wiki/File:Russian_booster_pump_IMG_0417.JPG) |
| Máy sấy nông sản 12 khay | `may-say.jpg` | Désicateur.jpg | Mait123 | CC0 | [Commons](https://commons.wikimedia.org/wiki/File:D%C3%A9sicateur.jpg) |
| Máy cắt tỉa hàng rào chạy xăng | `may-cat-canh.jpg` | Hedge trimmer with white flowers.jpg | Ben Grey | CC BY-SA 2.0 | [Commons](https://commons.wikimedia.org/wiki/File:Hedge_trimmer_with_white_flowers.jpg) |
| Gạo lứt huyết rồng (túi 2kg) | `gao-lut.jpg` | Germinated brown rice - long grain.jpg | Chouk Khmer | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Germinated_brown_rice_-_long_grain.jpg) |
| Nếp cái hoa vàng (túi 2kg) | `nep-cai-hoa-vang.jpg` | 2014 uncooked Thai glutinous rice.jpg | Takeaway | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:2014_uncooked_Thai_glutinous_rice.jpg) |
| Cà phê nhân Robusta (gói 500g) | `ca-phe-robusta.jpg` | Another closer look at the roasted coffee beans.jpg | Jameswasswa | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Another_closer_look_at_the_roasted_coffee_beans.jpg) |
| Hạt tiêu đen Phú Quốc (hũ 200g) | `tieu-den.jpg` | Black Pepper.jpg | Prosthetic Head | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Black_Pepper.jpg) |
| Mật ong hoa nhãn (chai 500ml) | `mat-ong.jpg` | 20140910-ARS-LSC-01361 (15083514808).jpg | U.S. Department of Agriculture

Lance Cheung/Multimedia PhotoJournalis | Public domain | [Commons](https://commons.wikimedia.org/wiki/File:20140910-ARS-LSC-01361_(15083514808).jpg) |
| Hạt điều rang muối (hũ 500g) | `hat-dieu.jpg` | 9522Anacardium occidentale in the Philippines 02.jpg | Judgefloro | CC0 | [Commons](https://commons.wikimedia.org/wiki/File:9522Anacardium_occidentale_in_the_Philippines_02.jpg) |
| Chè xanh Thái Nguyên (gói 200g) | `tra-xanh.jpg` | Green tea leaves.jpg | たね | CC BY-SA 3.0 | [Commons](https://commons.wikimedia.org/wiki/File:Green_tea_leaves.jpg) |
| Xoài cát Hoà Lộc (kg) | `xoai-cat.jpg` | Mangos - single and halved.jpg | Ivar Leidus | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Mangos_-_single_and_halved.jpg) |
| Thanh long ruột đỏ (kg) | `thanh-long.jpg` | Drachenfrucht 2010.jpg | Benutzer:MBq | Public domain | [Commons](https://commons.wikimedia.org/wiki/File:Drachenfrucht_2010.jpg) |
| Sầu riêng Ri6 (kg) | `sau-rieng.jpg` | Durian 4.jpg | Tris_T7 | CC BY-SA 3.0 | [Commons](https://commons.wikimedia.org/wiki/File:Durian_4.jpg) |
| Dừa xiêm xanh Bến Tre (trái) | `dua-xiem.jpg` | ARUBA-eidechsen-kokos.jpg | Bgabel | CC BY-SA 3.0 | [Commons](https://commons.wikimedia.org/wiki/File:ARUBA-eidechsen-kokos.jpg) |
| Bưởi da xanh Bến Tre (trái) | `buoi-da-xanh.jpg` | Citrus maxima piece.jpg | George Shuklin | CC BY-SA 1.0 | [Commons](https://commons.wikimedia.org/wiki/File:Citrus_maxima_piece.jpg) |
