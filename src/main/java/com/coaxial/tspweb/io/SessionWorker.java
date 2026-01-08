package com.coaxial.tspweb.io;

import com.coaxial.tspweb.common.StatusCode;
import com.coaxial.tspweb.io.reqRep.ClientRequest;
import com.coaxial.tspweb.model.Solver;
import com.coaxial.tspweb.model.SolverResult;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

public class SessionWorker {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final WebSocketSession session;
    private final Gson gson = new Gson();

    public SessionWorker(WebSocketSession session) {
        this.session = session;
    }

    /**
     * Hàm nhận yêu cầu từ SocketHandler.
     * Tạo một luồng mới để chạy Solver, tránh làm chặn luồng WebSocket chính.
     */
    public void onMessage(String payload) {
        new Thread(() -> {
            try {
                // 1. Parse dữ liệu đầu vào từ Client
                ClientRequest request = gson.fromJson(payload, ClientRequest.class);

                // 2. Khởi tạo và gọi Solver
                Solver solver = new Solver();
                
                // Truyền 'this' (SessionWorker) vào để Solver có thể báo cáo tiến độ (tellStatus)
                SolverResult result = solver.exactSolve(request, this);

                // 3. Gửi kết quả cuối cùng (QUAN TRỌNG)
                if (result != null) {
                    sendResult(result);
                } else {
                    tellStatus(StatusCode.ERROR, "Không tìm thấy đường đi khả thi.");
                }

            } catch (Exception e) {
                log.error("Lỗi trong quá trình xử lý", e);
                tellStatus(StatusCode.ERROR, "Lỗi Server: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Gửi kết quả cuối cùng về Client.
     * Bắt buộc phải thêm field "type": "RESULT" để main.js nhận diện.
     */
    public void sendResult(SolverResult result) {
        // Lấy GsonWrapper từ kết quả (chứa path, distance...)
        GsonWrapper wrapper = result.toGsonWrapper();
        
        // --- QUAN TRỌNG: Thêm định danh để Frontend vẽ bản đồ ---
        wrapper.add("type", "RESULT"); 
        
        // Gửi đi
        sendMessage(wrapper.toString());
    }

    /**
     * Gửi trạng thái (Loading, Calculating...) về Client.
     * Solver sẽ gọi hàm này để cập nhật tiến độ.
     */
    public void tellStatus(StatusCode code, String message, long progress) {
        GsonWrapper wrapper = new GsonWrapper();
        wrapper.add("type", "STATUS")
               .add("code", code.toString())
               .add("message", message);
               
        if (progress >= 0) {
            wrapper.add("progress", progress);
        }
        
        sendMessage(wrapper.toString());
    }

    // Nạp chồng hàm tellStatus cho trường hợp không có progress
    public void tellStatus(StatusCode code, String message) {
        tellStatus(code, message, -1);
    }
    
    // Nạp chồng hàm tellStatus cho trường hợp chỉ có code (ví dụ DONE)
    public void tellStatus(StatusCode code) {
        tellStatus(code, "", -1);
    }

    /**
     * Hàm gửi tin nhắn cơ sở qua WebSocket.
     * Cần synchronized để tránh lỗi khi Solver (đa luồng) gửi tiến độ cùng lúc.
     */
    private synchronized void sendMessage(String msg) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(msg));
            }
        } catch (IOException e) {
            log.error("Không thể gửi tin nhắn tới Client: " + e.getMessage());
        }
    }
}