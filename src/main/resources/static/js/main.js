// main.js - Phiên bản Final v1004 (Logic lấy index thực tế)

// 1. Cấu hình Bản đồ
var map = L.map('map').setView([21.0285, 105.8542], 13);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '© OpenStreetMap'
}).addTo(map);

// 2. Biến toàn cục
var markers = [];      
var routeLayers = []; 
var socket = new WebSocket("ws://" + location.host + "/msg");

console.log("--- ĐÃ TẢI CODE MỚI (v1004 - FIX LABEL BẰNG INDEX THỰC) ---");

// 3. Tạo Icon
function createPinIcon(number, styleType) {
    let cssClass = 'pin-head ';
    let bgColor = '';
    
    if (styleType === 'waiting') cssClass += 'pin-waiting';
    else if (styleType === 'start') cssClass += 'pin-start';
    else {
        cssClass += 'pin-result';
        bgColor = `background-color: #007bff; border-color: #fff;`; 
    }

    let displayNum = (number === 0) ? '?' : number;
    return L.divIcon({
        className: 'marker-wrapper', 
        html: `<div class="${cssClass}" style="${bgColor}"><span class="pin-number">${displayNum}</span></div>`,
        iconSize: [40, 40], iconAnchor: [20, 20], popupAnchor: [0, -50]
    });
}

// 4. Xử lý Click & Marker
map.on('click', function(e) {
    if (routeLayers.length > 0) clearRouteOnly(); 
    addMarker(e.latlng);
});

function addMarker(latlng) {
    let marker = L.marker(latlng, {
        icon: createPinIcon(markers.length + 1, 'waiting'),
        draggable: true
    }).addTo(map);

    marker.on('click', () => {
        map.removeLayer(marker);
        markers = markers.filter(m => m !== marker);
        markers.forEach((m, idx) => m.setIcon(createPinIcon(idx + 1, 'waiting')));
        if (routeLayers.length > 0) clearRouteOnly();
        updateUI();
    });

    marker.on('dragend', () => { if (routeLayers.length > 0) clearRouteOnly(); });
    markers.push(marker);
    updateUI();
}

function updateUI() {
    document.getElementById('point-count').innerText = `Đã chọn: ${markers.length} điểm`;
    if (markers.length < 2) document.getElementById('route-details').style.display = 'none';
}

function clearMap() {
    markers.forEach(m => map.removeLayer(m));
    clearRouteOnly(); 
    markers = [];
    updateUI();
    document.getElementById('status').innerText = "Sẵn sàng";
}

function clearRouteOnly() {
    if (routeLayers.length > 0) {
        routeLayers.forEach(layer => {
            if (map.hasLayer(layer)) map.removeLayer(layer);
        });
        routeLayers = []; 
    }
    document.getElementById('steps-container').innerHTML = '';
    document.getElementById('route-details').style.display = 'none';
    
    markers.forEach((m, idx) => m.setIcon(createPinIcon(idx + 1, 'waiting')));
}

// 5. Gửi Yêu Cầu
function calculateRoute() {
    if (markers.length < 2) { alert("Cần ít nhất 2 điểm!"); return; }
    
    clearRouteOnly();
    document.getElementById('status').innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang tính toán...';
    let locs = markers.map(m => ({lat: m.getLatLng().lat, lng: m.getLatLng().lng}));
    
    if (socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({
            locations: locs,
            energy: { consumption: 0, interpolation: false, additional: {threshold:0, value:0}, recuperation: {threshold:0, value:0} }
        }));
    } else alert("Mất kết nối Server. F5 lại trang.");
}

// 6. Nhận Kết Quả
socket.onmessage = function(event) {
    let msg = JSON.parse(event.data);
    if (msg.type === "RESULT") {
        document.getElementById('status').innerHTML = '<i class="fas fa-paint-brush"></i> Đang vẽ...';
        processResult(msg);
    } else if (msg.type === "STATUS") {
        if(msg.message) document.getElementById('status').innerText = msg.message;
    }
};

async function processResult(msg) {
    let path = msg.path;
    let startIdx = path.indexOf(0);
    // Đảm bảo path là vòng kín bắt đầu từ 0
    if (startIdx > 0 && startIdx < path.length - 1) {
        let unique = path.slice(0, path.length - 1);
        path = unique.slice(startIdx).concat(unique.slice(0, startIdx));
        path.push(0); 
    }

    // Đổi màu Marker
    for (let i = 0; i < path.length - 1; i++) {
        let m = markers[path[i]];
        m.setIcon(createPinIcon(i + 1, (i===0 ? 'start' : 'result')));
        m.setZIndexOffset(1000 - i); 
    }
    
    await drawColorfulRoute(path);
}

// 7. Vẽ Đường (Logic chuẩn xác nhất)
async function drawColorfulRoute(pathIndices) {
    let totalKm = 0;
    let stepsHTML = "";
    document.getElementById('route-details').style.display = 'block';

    for (let i = 0; i < pathIndices.length - 1; i++) {
        let fromIdx = pathIndices[i];     // Index thực trong mảng markers (0, 1, 2...)
        let toIdx = pathIndices[i+1];     // Index thực của điểm đến

        let fromM = markers[fromIdx];
        let toM = markers[toIdx];

        try {
            let url = `https://routing.openstreetmap.de/routed-car/route/v1/driving/${fromM.getLatLng().lng},${fromM.getLatLng().lat};${toM.getLatLng().lng},${toM.getLatLng().lat}?overview=full&geometries=geojson`;
            
            let res = await fetch(url);
            let json = await res.json();
            
            if(json.code === 'Ok') {
                let route = json.routes[0];
                totalKm += route.distance / 1000;
                let latlngs = route.geometry.coordinates.map(c => [c[1], c[0]]);

                let poly = L.polyline(latlngs, {
                    color: '#007bff', 
                    weight: 6,        
                    opacity: 0.9,
                    lineJoin: 'round'
                }).addTo(map);

                routeLayers.push(poly);

                // --- SỬA Ở ĐÂY: DÙNG SỐ THỨ TỰ THẬT TỪ MẢNG ---
                // Cộng thêm 1 để hiển thị (ví dụ index 0 -> điểm số 1)
                let labelFrom = fromIdx + 1;
                let labelTo = toIdx + 1;

                stepsHTML += `<div class="step-row">
                    <span>
                        <span class="color-tag" style="background:#007bff">Chặng ${i+1}</span>
                        <b style="color: #2d3436;">${labelFrom} &rarr; ${labelTo}</b>
                    </span>
                    <span class="step-dist">${(route.distance/1000).toFixed(2)} km</span>
                </div>`;
            }
        } catch(e) {
            console.error("Lỗi vẽ:", e);
            let line = L.polyline([fromM.getLatLng(), toM.getLatLng()], {color: '#007bff', dashArray: '5,10'}).addTo(map);
            routeLayers.push(line);
        }
    }

    document.getElementById('steps-container').innerHTML = stepsHTML;
    document.getElementById('total-dist').innerText = totalKm.toFixed(2);
    document.getElementById('status').innerText = 'Hoàn tất!';
    
    if(routeLayers.length > 0) map.fitBounds(L.featureGroup(routeLayers).getBounds(), {padding: [50, 50]});
}