package com.coaxial.tspweb.common;

import com.coaxial.tspweb.io.GsonWrapper;

/**
 * Đại diện cho loại tin nhắn được gửi đến máy khách. Có thể là một thông báo trạng thái về tiến trình hoặc
 * thông báo chứa kết quả.
 */
public enum MessageType
{
    STATUS("STATUS"),
    RESULT("RESULT");

    private final String type;

    MessageType(final String code)
    {
        this.type = code;
    }

    @Override
    public String toString()
    {
        return type;
    }

    public GsonWrapper toGsonWrapper(GsonWrapper wrapper)
    {
        return (wrapper == null ? new GsonWrapper() : wrapper).add("type", type);
    }

    public GsonWrapper toGsonWrapper()
    {
        return toGsonWrapper(null);
    }
}
