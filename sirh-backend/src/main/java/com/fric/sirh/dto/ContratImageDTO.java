// ContratImageDTO.java
package com.fric.sirh.dto;

import lombok.Data;
import java.util.List;

@Data
public class ContratImageDTO {
    private String contratId;
    private List<String> imageUrls;
}