package za.co.sqim.vitality.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.time.Duration;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author Avuyile Malotana
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = false, exclude = { "receivingUser", "consentingUser",
    "activityMaster" })
@Entity
@EntityListeners({ AuditingEntityListener.class })
@Table(name = "ACTIVITY")
public class Activity extends BaseEntity
{
  public static final double AVERAGE_DAYS_PER_MONTH = 365.0 / 12.0;
  
  public enum Status {
    NOT_AVAILABLE_YET, CLOSED_COMPLETE, CLOSED_INCOMPLETE, OPEN_COMPLETE, OPEN_INCOMPLETE
  }
  
  public enum ExpiryPeriod {
    
    // @formatter:off
    ONE_DAY(1),
    ONE_WEEK(7),
    TWO_WEEKS(14),
    ONE_MONTH(AVERAGE_DAYS_PER_MONTH),
    TWO_MONTHS(AVERAGE_DAYS_PER_MONTH * 2),
    THREE_MONTHS(AVERAGE_DAYS_PER_MONTH * 3),
    SIX_MONTHS(AVERAGE_DAYS_PER_MONTH * 6),
    ONE_YEAR(365),
    NEVER(0),
    CUSTOM(-1);
    // @formatter:on
    
    private final int days;
    
    private ExpiryPeriod(int days) {
      this.days = days;
    }
    
    private ExpiryPeriod(double days) {
      this.days = (int) (days + 0.5);
    }
    
    public int getDays() {
      return days;
    }
    
    public String getAbbreviation() {
      switch (this) {
      case CUSTOM:
        return "c";
      case NEVER:
        return "&#x221e;";
      case ONE_DAY:
        return "1d";
      case ONE_MONTH:
        return "1m";
      case ONE_WEEK:
        return "1w";
      case ONE_YEAR:
        return "1y";
      case SIX_MONTHS:
        return "6m";
      case THREE_MONTHS:
        return "3m";
      case TWO_MONTHS:
        return "2m";
      case TWO_WEEKS:
        return "2w";
      default:
        break;
      }
      return "c";
    }
  }
  
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "ID", nullable = false, unique = true, updatable = false)
  private Long id;
  
  /**
   * Rules:
   * (1) receivingUser must belong to this level or a sub group of this level
   * (2) never null
   * (3) if activityMaster != null then level = activityMaster.level
   */
  @ManyToOne
  @JoinColumn(name = "LEVEL_ID", nullable = false)
  @NotNull
  private Level level;
  
  /**
   * Rules:
   * (1) receivingUser must belong to this group
   * (2) never null (new field hence @NotNull is missing)
   * (3) if activityMaster != null then group = activityMaster.group
   * (4) updatable = false
   */
  @ManyToOne
  @JoinColumn(name = "GROUP_ID") // , nullable = false, updatable = false)
  // @NotNull
  private Group group;
  
  @ManyToOne
  @JoinColumn(name = "RECEIVING_USER", nullable = false, updatable = false)
  @NotNull
  protected User receivingUser = null;
  
  @ManyToOne
  @JoinColumn(name = "CONSENTING_USER", nullable = false, updatable = false)
  @NotNull
  protected User consentingUser = null;
  
  /**
   * Rules:
   * (1) can be null
   */
  @ManyToOne
  @JoinColumn(name = "ACTIVITY_MASTER", updatable = false)
  protected ActivityMaster activityMaster;
  
  @Column(name = "NAME")
  @NotEmpty
  private String name;
  
  // TODO change length of description in databases
  @Column(name = "DESCRIPTION")
  @NotEmpty
  @Length(max = 255)
  private String description;
  
  @Column(name = "POINTS")
  private int points = 1;
  
  @Column(name = "POINTS_EXPIRE_IN_DAYS")
  private long pointsExpireInDays = ExpiryPeriod.SIX_MONTHS.days;
  
  @Column(name = "COMPLETED")
  private boolean completed = false;
  
  @Column(name = "COMPLETED_DATE")
  private LocalDateTime completedDate = null;
  
  @Column(name = "POINTS_ASSIGNED")
  private boolean pointsAssigned = false;
  
  @Column(name = "POINTS_ASSIGNED_DATE")
  private LocalDateTime pointsAssignedDate = null;
  
  @Deprecated
  @CreatedDate
  @Column(name = "CREATION_DATE")
  private LocalDateTime creationDate = LocalDateTime.now();
  
  @DateTimeFormat(pattern = "yyyy/MM/dd HH:mm")
  // @NotNull
  @Column(name = "ACTIVE_FROM")
  private LocalDateTime activeFrom = null;
  
  // can be null, if not null then this must be greater than or equal to start
  @DateTimeFormat(pattern = "yyyy/MM/dd HH:mm")
  // @NotNull
  @Column(name = "ACTIVE_TO")
  private LocalDateTime activeTo = null;
  
  // utility functions
  
  public void setGroupAndLevel(Group group) {
    if (group == null) {
      this.group = null;
      this.level = null;
    }
    else {
      this.group = group;
      this.level = group.getLevel();
    }
  }
  
  /// Transient properties
  
  @Transient
  private long pointsTotal;
  
  /**
   * Used in the getStatus() and isActive() calculations.
   */
  @Transient
  private LocalDateTime statusDateTime = LocalDateTime.now();
  
  // Custom Getters and Setters
  
  public void setCompleted(boolean completed) {
    this.completed = completed;
    if (completed && this.completedDate == null) {
      completedDate = LocalDateTime.now();
    }
  }
  
  /// other
  
  public Activity.Status getStatus() {
    if (statusDateTime == null) {
      statusDateTime = LocalDateTime.now();
    }
    if (activeFrom != null && statusDateTime.isBefore(activeFrom)) {
      return Status.NOT_AVAILABLE_YET;
    }
    if (completed && pointsAssigned) {
      return Status.CLOSED_COMPLETE;
    }
    if (!completed && activeTo != null && statusDateTime.isAfter(activeTo)) {
      return Status.CLOSED_INCOMPLETE;
    }
    if (!completed) {
      return Status.OPEN_INCOMPLETE;
    }
    else {
      return Status.OPEN_COMPLETE;
    }
  }
  
  public boolean isActive() {
    boolean ret = true;
    if (activeFrom != null) {
      ret &= activeFrom.isBefore(statusDateTime);
    }
    if (activeTo != null) {
      ret &= activeTo.isAfter(statusDateTime);
    }
    return ret;
  }
  
  public Duration getExpiryDuration() {
    return Duration.between(statusDateTime,
        activeTo == null ? LocalDateTime.MAX : activeTo);
  }
  
  public ExpiryPeriod getExpiryPeriod() {
    for (ExpiryPeriod period : ExpiryPeriod.values()) {
      if (period.getDays() == this.pointsExpireInDays) {
        return period;
      }
    }
    return ExpiryPeriod.CUSTOM;
  }
  
  public Long getLevelId() {
    return this.level == null ? null : this.level.getId();
  }
  
  public Long getReceivingUserId() {
    return this.receivingUser == null ? null : this.receivingUser.getId();
  }
  
  public Long getConsentingUserId() {
    return this.consentingUser == null ? null : this.consentingUser.getId();
  }
  
  /**
   * Get the LocalDateTime to use with the first (the positive) LevelUpdate when
   * assigning points.
   * 
   * @return
   */
  public LocalDateTime getLevelUpdateEntryTimestamp() {
    LocalDateTime entryTimestamp = LocalDateTime.now();
    if (activeFrom != null && entryTimestamp.isAfter(activeFrom)) {
      entryTimestamp = activeFrom;
    }
    if (activeTo != null && entryTimestamp.isAfter(activeTo)) {
      entryTimestamp = activeTo;
    }
    if (getCreatedDate() != null && entryTimestamp.isAfter(getCreatedDate())) {
      entryTimestamp = getCreatedDate();
    }
    return entryTimestamp;
  }
  
  /**
   * Make sure the end date is always after start (swaps them if need be).
   */
  public void swapStartAndEndIfNeedBe() {
    if (activeFrom == null || activeTo == null)
      return;
    if (activeFrom.isAfter(activeTo)) {
      LocalDateTime t = this.activeFrom;
      this.activeFrom = this.activeTo;
      this.activeTo = t;
    }
  }
}