package com.butingbe.domain.zoneevent.entity;

/** City explorer grade calculated dynamically from earned zone titles. */
public enum CityGrade {
  BEGINNER,
  EXPLORER,
  MASTER,
  TRUE_BUSAN;

  /**
   * Compares the seniority of two city grades.
   *
   * @param other target grade to compare against
   * @return true if this grade is strictly higher than other
   */
  public boolean isHigherThan(CityGrade other) {
    if (other == null) {
      return true;
    }
    return this.ordinal() > other.ordinal();
  }
}
