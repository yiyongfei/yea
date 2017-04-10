/**
 * Copyright 2017 伊永飞
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yea.core.remote.struct;

import java.util.Map;

import com.yea.core.base.id.UUIDGenerator;

/**
 * 
 * @author yiyongfei
 * 
 */
public final class Header {
    private int crcCode = 0xabef0101;
    private int length;// 消息长度
    private byte[] sessionID;// 会话ID
    private byte type;// 消息类型
    private byte priority;// 消息优先级
    private byte result;// 消息结果
    private Map<String, Object> attachment;// 消息附属内容
    /**
     * @return the crcCode
     */
    public final int getCrcCode() {
        return crcCode;
    }

    /**
     * @param crcCode
     *            the crcCode to set
     */
    public final void setCrcCode(int crcCode) {
        this.crcCode = crcCode;
    }

    /**
     * @return the length
     */
    public final int getLength() {
        return length;
    }

    /**
     * @param length
     *            the length to set
     */
    public final void setLength(int length) {
        this.length = length;
    }

    /**
     * @return the sessionID
     */
    public final byte[] getSessionID() {
        return sessionID;
    }

    /**
     * @param sessionID
     *            the sessionID to set
     */
    public final void setSessionID(byte[] sessionID) {
        this.sessionID = sessionID;
    }

    /**
     * @return the type
     */
    public final byte getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public final void setType(byte type) {
        this.type = type;
    }

    /**
     * @return the priority
     */
    public final byte getPriority() {
        return priority;
    }

    /**
     * @param priority
     *            the priority to set
     */
    public final void setPriority(byte priority) {
        this.priority = priority;
    }

    public byte getResult() {
        return result;
    }

    public void setResult(byte result) {
        this.result = result;
    }

    public Map<String, Object> getAttachment() {
        return attachment;
    }

    public void setAttachment(Map<String, Object> attachment) {
        this.attachment = attachment;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Header [").append("crcCode=").append(crcCode).append(", ");
        stringBuffer.append("length=").append(length).append(", ");
        stringBuffer.append("sessionID=").append(UUIDGenerator.restore(sessionID)).append(", ");
        stringBuffer.append("type=").append(type).append(", ");
        stringBuffer.append("priority=").append(priority).append(", ");
        stringBuffer.append("result=").append(result);
        if (getAttachment() != null && getAttachment().size() > 0) {
            stringBuffer.append(", ").append("Attachment=[");
            for (Map.Entry<String, Object> param : getAttachment().entrySet()) {
                stringBuffer.append(param.getKey()).append("=").append(param.getValue()).append(", ");
            }
            stringBuffer.append("]");
        }
        stringBuffer.append("]");
        return stringBuffer.toString();
    }

}
