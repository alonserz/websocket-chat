package org.example.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientRequest {
    public String type;
    public String message;
    public String uuid;
}
