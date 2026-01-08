package com.coaxial.tspweb.io;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * Một trình bao bọc đơn giản xung quanh Gson cung cấp một cơ chế chuỗi phương thức giúp đơn giản hóa việc tạo các đối tượng json đơn giản.
 * Nó cũng cung cấp một phương thức để đưa quá trình gửi vào chuỗi phương thức.
 */
public class GsonWrapper
{
    private JsonObject object;

    public GsonWrapper()
    {
        object = new JsonObject();
    }

    /**
     * Thêm một cặp khóa-giá trị chuỗi vào đối tượng json.
     *
     * @param key the key
     * @param val the value
     * @return this GsonWrapper for method chaining
     */
    public GsonWrapper add(String key, String val)
    {
        object.addProperty(key, val);
        return this;
    }

    /**
     * Thêm một cặp khóa-giá trị Số vào đối tượng json.
     *
     * @param key the key
     * @param val the value
     * @return this GsonWrapper for method chaining
     */
    public GsonWrapper add(String key, Number val)
    {
        object.addProperty(key, val);
        return this;
    }

    /**
     * Thêm cặp khóa-giá trị Mảng Số vào đối tượng json.
     *
     * @param key the key
     * @param val the value
     * @return this GsonWrapper for method chaining
     */
    public GsonWrapper add(String key, Number[] val)
    {
        JsonArray array = new JsonArray(val.length);
        for(Number n:val)
            array.add(n);
        object.add(key, array);
        return this;
    }

    /**
     * Thêm một cặp khóa-giá trị boolean vào đối tượng json.
     *
     * @param key the key
     * @param val the value
     * @return this GsonWrapper for method chaining
     */
    public GsonWrapper add(String key, boolean val)
    {
        object.addProperty(key, val);
        return this;
    }

    /**
     * Thêm một JsonElement tùy chỉnh vào đối tượng json này.
     * @param key the key
     * @param element the element
     * @return this GsonWrapper for method chaining
     */
    public GsonWrapper add(String key, JsonElement element)
    {
        object.add(key, element);
        return this;
    }

    /**
     * Thêm một cặp khóa-giá trị chuỗi vào đối tượng json nếu điều kiện đã cho là đúng.
     *
     * @param key the key
     * @param val the value
     * @return this GsonWrapper for method chaining
     */
    public GsonWrapper addIf(boolean condition, String key, String val)
    {
        if (condition)
            add(key, val);
        return this;
    }

    /**
     * Thêm một cặp khóa-giá trị Số vào đối tượng json nếu điều kiện đã cho là đúng.
     *
     * @param key the key
     * @param val the value
     * @return this GsonWrapper for method chaining
     */
    public GsonWrapper addIf(boolean condition, String key, Number val)
    {
        if (condition)
            add(key, val);
        return this;
    }

    /**
     * Thêm một cặp khóa-giá trị boolean vào đối tượng json nếu điều kiện đã cho là đúng.
     *
     * @param key the key
     * @param val the value
     * @return this GsonWrapper for method chaining
     */
    public GsonWrapper addIf(boolean condition, String key, boolean val)
    {
        if (condition)
            add(key, val);
        return this;
    }

    /**
     * Gửi đối tượng json dưới dạng tin nhắn văn bản qua WebSocketSession đã cho.
     * Đây sẽ là lệnh gọi cuối cùng trong chuỗi phương thức.
     *
     * @param session the session to send the json object over
     * @return true if sending the message was successful; false otherwise
     */
    public boolean send(WebSocketSession session)
    {
        try
        {
            synchronized (session)
            {
                session.sendMessage(new TextMessage(new Gson().toJson(object)));
            }
            return true;
        } catch (IOException | IllegalStateException e)
        {
            return false;
        }
    }

    /**
     * Ghi nội dung của trình bao bọc này vào bảng điều khiển (Thông tin cấp) bằng cách sử dụng phiên bản trình ghi đã cho.
     * @param message the message to prepend
     * @param logger the logger instance to use
     * @return this GsonWrapper for method chaining
     */
    public GsonWrapper log(String message, Logger logger)
    {
        logger.info(message + new Gson().toJson(object));
        return this;
    }
    /**
     * Chuyển đổi đối tượng JsonObject bên trong thành chuỗi String.
     * Cần thiết để SessionWorker có thể gọi wrapper.toString().
     */
    @Override
    public String toString() {
        // Chuyển đối tượng JSON thành chuỗi để gửi qua WebSocket
        return new com.google.gson.Gson().toJson(object);
    }
}
