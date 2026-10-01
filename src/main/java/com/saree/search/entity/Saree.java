package com.saree.search.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sarees", uniqueConstraints = @UniqueConstraint(columnNames = "image_name"))
public class Saree {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "image_name", nullable = false, unique = true) private String imageName;
  @Column(name = "image_path", nullable = false) private String imagePath;
  @Column(name = "primary_color", nullable = false) private String primaryColor;
  @Column(name = "secondary_color") private String secondaryColor;
  @Column(name = "primary_color_percentage", nullable = false) private double primaryColorPercentage;
  @Column(name = "secondary_color_percentage") private double secondaryColorPercentage;
  public Long getId() { return id; } public String getImageName() { return imageName; } public String getImagePath() { return imagePath; }
  public String getPrimaryColor() { return primaryColor; } public String getSecondaryColor() { return secondaryColor; }
  public double getPrimaryColorPercentage() { return primaryColorPercentage; } public double getSecondaryColorPercentage() { return secondaryColorPercentage; }
  public void setImageName(String v) { imageName = v; } public void setImagePath(String v) { imagePath = v; }
  public void setPrimaryColor(String v) { primaryColor = v; } public void setSecondaryColor(String v) { secondaryColor = v; }
  public void setPrimaryColorPercentage(double v) { primaryColorPercentage = v; } public void setSecondaryColorPercentage(double v) { secondaryColorPercentage = v; }
}
