package za.co.sqim.vitality.model;

import static javax.persistence.GenerationType.IDENTITY;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;

import za.co.sqim.vitality.model.constraint.Color;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Avuyile Malotana
 */
@Getter
@Setter
@Entity
@Table(name = "LEVEL_MILESTONES")
@EqualsAndHashCode(of = { "level", "points" })
@ToString(of = { "name", "points" })
public class Milestone
{
  @JsonIgnore
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "ID", nullable = false, unique = true, updatable = false)
  private Long id;
  
  // NB The level can't be updated, otherwise the data/model might be corrupted.
  @JsonIgnore
  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "LEVEL_ID", updatable = false)
  @NotNull
  private Level level;
  
  @Column(name = "NAME", nullable = false)
  @NotBlank(message = Constants.Field.REQUIRED)
  private String name;
  
  @Column(name = "DESCRIPTION", nullable = false)
  @NotBlank(message = Constants.Field.REQUIRED)
  @Length(max = 255)
  private String description;
  
  // TODO Discuss if there should be a min == 0 here?
  @Column(name = "POINTS", nullable = false)
  @Min(0)
  private int points;
  
  @Column(name = "COLOR")
  @Color
  @NotNull
  private String color;
}