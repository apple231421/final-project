package THE_JEONG.Hospital.dto;

import lombok.Data;

@Data
public class ReservationRequestDto {
    private String date;
    private String time;
    private String department;
    private Long doctorId;
}
