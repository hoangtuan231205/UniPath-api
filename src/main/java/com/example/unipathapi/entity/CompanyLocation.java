package com.example.unipathapi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
// 👇 IMPORT CỰC KỲ QUAN TRỌNG ĐỂ JAVA HIỂU ĐƯỢC TỌA ĐỘ
import org.locationtech.jts.geom.Point;

@Data // Tự động tạo Getter, Setter nhờ Lombok
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "company_locations")
public class CompanyLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Tạm thời map id công ty dạng số. Khi nào tạo bảng Company xong chúng ta sẽ nối khóa ngoại (@ManyToOne)
    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "address", columnDefinition = "TEXT", nullable = false)
    private String address;

    // 👇 ĐÂY LÀ "LINH HỒN" CỦA HỆ THỐNG GIS
    @Column(name = "geom", columnDefinition = "geometry(Point,4326)")
    private Point geom;
}
