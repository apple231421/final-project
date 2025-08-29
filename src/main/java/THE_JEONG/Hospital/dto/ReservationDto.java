package THE_JEONG.Hospital.dto;

import lombok.Data;

@Data
public class ReservationDto {
    private String date;
    private String time;
    private Long doctorId;
    private String department;
}
