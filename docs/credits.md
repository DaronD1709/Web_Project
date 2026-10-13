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
