package za.co.sqim.vitality.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Avuyile Malotana
 */
@Getter
@Setter
@EqualsAndHashCode(of = { "code" })
@ToString(of = { "id", "name", "code" })
@Entity
@EntityListeners({ AuditingEntityListener.class })
@Table(name = "GROUPS", indexes = { @Index(columnList = "code") })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Group
{
  public static final String CODE_PATTERN = "^(\\d|\\w|-)+$";
  
  private static final Logger logger = LoggerFactory.getLogger(Group.class);
  
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "ID", nullable = false, unique = true, updatable = false)
  private Long id;
  
  /**
   * A unique code to identify this group, will be used in user friendly
   * URLs, for example olympia.com/group/details/dev where "dev" is the
   * unique code.
  
   */
  @Column(name = "CODE", unique = true, nullable = true)
  @Pattern(regexp = CODE_PATTERN)
  @NotEmpty
  private String code = "ugid-" + UUID.randomUUID().toString();
  
  @ManyToOne(cascade = {})
  @JoinColumn(name = "PARENT_ID", nullable = true)
  private Group parent = null;
  
  @Column(name = "NAME", nullable = false)
  @NotEmpty
  private String name = "";
  
  @Column(name = "DESCRIPTION", nullable = false, length = 2048)
  @NotNull
  private String description = "";
  
  /** Order for Groups with same parent. */
  @Column(name = "SORT_ORDER")
  @NotNull
  private int order = 0;
  
  /** Order by parents first, etc. i.e. hierarchical order. */
  @Column(name = "NATURAL_ORDER")
  private Integer naturalOrder = 0;
  
  @ManyToOne(cascade = {})
  @CreatedBy
  private User createdBy = null;
  
  @CreatedDate
  private LocalDateTime createdDate = LocalDateTime.now();
  
  @ManyToOne(cascade = {})
  @LastModifiedBy
  private User lastModifiedBy = null;
  
  @LastModifiedDate
  private LocalDateTime lastModifiedDate = null;
  
  @Column(name = "DELETED")
  private Boolean deleted = null;
  
  @Column(name = "DELETED_DATE")
  private LocalDateTime deletedDateTime = null;
  
  @ManyToOne(cascade = {})
  @JoinColumn(name = "DELETED_BY", nullable = true)
  private User deletedBy = null;
  
  /**
   * Flag to show if User(s) can be added to this Group or not.
   * 
   * Useful if you want to create a Group that merely helps with structure or
   * organisation, for example: creating the Group <em>User Role</em> can then
   * be used to group <em>Developer</em> and <em>Tester</em>, without allowing
   * direct direct membership to <em>User Role</em>.
   */
  @Column(name = "ALLOW_DIRECT_MEMBERSHIP")
  private boolean allowDirectMembership = true;
  
  @OneToMany(mappedBy = "group", cascade = { CascadeType.REMOVE })
  private List<GroupMember> members = new ArrayList<>();
  
  @OneToMany(mappedBy = "group", cascade = { CascadeType.REMOVE })
  private List<ActivityMaster> activityMasters = new ArrayList<>();
  
  @OneToMany(mappedBy = "parent", cascade = { CascadeType.REMOVE })
  @OrderBy("order, name")
  private List<Group> children = new ArrayList<>();
  
  /**
   * @TODO Remove Used on the User form, not sure if this is correct place for
   *       this.
   */
  @Transient
  private boolean checked = false;
  
  /**
   * Custom setter for the parent group.
   * 
   * @throws IllegalStateException
   *           When a circular references is detected.
   */
  public void setParent(Group parent) {
    final String ERROR_MSG = "Can't assign group as parent, circular reference not allowed.";
    if (parent == null) {
      if (this.parent != null) {
        this.parent.children.remove(this);
      }
      this.parent = null;
      return;
    }
    if (parent == this) {
      throw new IllegalStateException(ERROR_MSG);
    }
    if (isDescendant(parent)) {
      throw new IllegalStateException(ERROR_MSG);
    }
    this.parent = parent;
    parent.children.add(this);
  }
  
  /**
   * Set the Group name, also the group code if the code is null or empty.
   * 
   * @param name
   */
  public void setName(String name) {
    this.name = name;
  }
  
  /**
   * Custom getter for natural order.
   */
  public int getNaturalOrder() {
    if (naturalOrder == null) {
      return 0;
    }
    else {
      return naturalOrder;
    }
  }
  
  /**
   * Custom getter for deleted variable.
   */
  public boolean getDeleted() {
    return this.deleted == null ? false : this.deleted;
  }
  
  /**
   * Short cut function.
   */
  public boolean isDeleted() {
    return this.getDeleted();
  }
  
  /**
   * Can this group be undeleted.
   */
  public boolean isRestorable() {
    Group a = this.parent;
    boolean restorable = this.getDeleted();
    while (a != null) {
      if (a.isDeleted()) {
        restorable = false;
      }
      a = a.parent;
    }
    return restorable;
  }
  
  /**
   * See if Group "test" is a descendant of this group.
   */
  public boolean isDescendant(Group test) {
    if (test == null) {
      return false;
    }
    for (Group child : children) {
      if (child.equals(test)) {
        return true;
      }
      if (child.isDescendant(test)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * A count of all descendant Groups.
   */
  public int getDescendantCount() {
    int count = this.getChildren().size();
    if (count == 0) {
      return count;
    }
    for (Group c : this.getChildren()) {
      count += c.getDescendantCount();
    }
    return count;
  }
  
  /**
   * Get all descendant Groups.
   */
  public List<Group> getDescendants() {
    List<Group> descendants = new ArrayList<>();
    descendants.addAll(children);
    for (Group child : children) {
      descendants.addAll(child.getDescendants());
    }
    return descendants;
  }
  
  /**
   * See if Group "test" is an ancestor of this group.
   */
  public boolean isAncestor(Group test) {
    if (test == null) {
      return false;
    }
    Group ancestor = this;
    do {
      ancestor = ancestor.parent;
      if (test.equals(ancestor)) {
        return true;
      }
    } while (ancestor != null);
    return false;
  }
  
  /**
   * Get the list of ancestors, starting at the root Group (group with no
   * parent).
   */
  public List<Group> getAncestors() {
    List<Group> ancestors = new ArrayList<>();
    Group group = this;
    while (group.parent != null) {
      ancestors.add(group.parent);
      group = group.parent;
    }
    Collections.reverse(ancestors);
    return ancestors;
  }
  
  /**
   * Get the the depth of this Group with regards to it's ancestors, a Group
   * with no parent will have a 0 depth.
   */
  public int getDepth() {
    int ret = 0;
    Group ancestor = this.parent;
    while (ancestor != null) {
      ret++;
      ancestor = ancestor.parent;
    }
    return ret;
  }
  
  /**
   * See if the User "test" is a direct member of this group, i.e. not through a
   * descendant group.
   */
  public boolean isMember(User test) {
    logger.debug("Testing to see if {} is a member of {}", test, this);
    if (test == null) {
      logger.debug("Null is never a member, returning false");
      return false;
    }
    for (GroupMember member : members) {
      logger.debug("Comparing {} against {}", test, member.getUser());
      if (member.getUser().equals(test)) {
        logger.debug("Found member, returning true");
        return true;
      }
    }
    logger.debug("Member not found, returning false");
    return false;
  }
  
  /**
   * See if the User "test" is a member of a sub group.
   */
  public boolean isMemberOfDescendant(User test) {
    if (this.children == null || this.children.size() == 0) {
      return false;
    }
    for (Group child : this.children) {
      if (child.isMember(test)) {
        return true;
      }
      if (child.isMemberOfDescendant(test)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * See if the User "test" is a member of an ancestor
   */
  public boolean isMemberOfAncestor(User test) {
    Group ancestor = this.parent;
    while (ancestor != null) {
      if (ancestor.isMember(test)) {
        return true;
      }
      ancestor = ancestor.parent;
    }
    return false;
  }
  
  /**
   * See if the User "test" is a member of this group or any of the descendant
   * groups.
   */
  public boolean isMemberOrMemberOfDescendant(User test) {
    if (isMember(test) || isMemberOfDescendant(test)) {
      return true;
    }
    else {
      return false;
    }
  }
  
  /**
   * See if the User "test" is a member of this group of any of the ancestor
   * groups.
   */
  public boolean isMemberOrMemberOfAncestor(User test) {
    if (isMember(test) || isMemberOfAncestor(test)) {
      return true;
    }
    else {
      return false;
    }
  }
  
  /**
   * Count all direct members (includes disabled and enabled)
   */
  public int getMemberCount() {
    return this.members == null ? 0 : this.members.size();
  }
  
  /**
   * Count all direct enabled members.
   */
  public int getEnabledMemberCount() {
    if (this.members == null)
      return 0;
    int c = 0;
    for (GroupMember gm : members) {
      if (gm.isEnabled())
        c++;
    }
    return c;
  }
  
  /**
   * Return all enabled members.
   */
  public List<GroupMember> getEnabledMembers() {
    List<GroupMember> ret = new ArrayList<>();
    for (GroupMember gm : members) {
      if (gm.isEnabled())
        ret.add(gm);
    }
    return ret;
  }
  
  /**
   * Return the members as a list of users.
   */
  public List<User> getUsers() {
    List<User> users = new ArrayList<>();
    for (GroupMember gm : getMembers()) {
      users.add(gm.getUser());
    }
    return users;
  }
  
  /**
   * Count the enabled children groups.
   */
  public int getEnabledChildrenCount() {
    int ret = 0;
    if (children == null)
      return 0;
    for (Group child : children) {
      if (child.isEnabled())
        ret++;
    }
    return ret;
  }
  
  public List<Group> getEnabledChildren() {
    List<Group> ret = new ArrayList<>();
    if (children == null)
      return ret;
    for (Group child : children) {
      if (child.isEnabled())
        ret.add(child);
    }
    return ret;
  }
  
  /**
   * Count all of the activity masters.
   */
  public int getActivityMasterCount() {
    return this.activityMasters == null ? 0 : this.activityMasters.size();
  }
  
  /**
   * Construct a name consisting of this Groups name and the all of the
   * ancestors.
   */
  public String getLongName(String seperator) {
    StringBuilder builder = new StringBuilder();
    builder.append(this.name);
    Group parent = this.parent;
    while (parent != null) {
      builder.insert(0, seperator);
      builder.insert(0, parent.name);
      parent = parent.parent;
    }
    return builder.toString();
  }
  
  public String getLongName() {
    return this.getLongName(" / ");
  }
  
  /**
   * Find the top group or Level.
   */
  public Level getLevel() {
    if (this instanceof Level) {
      return (Level) this;
    }
    if (this.parent != null) {
      return this.parent.getLevel();
    }
    return null;
  }
  
  /**
   * Type of Group: 'Group', 'Level', 'Role' or 'Department'
   */
  public String getType() {
    return this.getClass().getSimpleName();
  }
  
  /**
   * Try and be a bit more intelligent regarding modified or not.
   */
  public boolean isModified() {
    // data missing, not modified (audting was added later so we need to look
    // after null data)
    if (this.lastModifiedBy == null || this.lastModifiedDate == null) {
      return false;
    }
    
    // if users are different, it is modified
    if (!this.lastModifiedBy.equals(this.createdBy))
      return true;
    
    // if no created date, treat as modified
    if (this.createdDate == null)
      return true;
    
    // allow 15 minute for quick fixes
    if (this.createdDate.isBefore(this.lastModifiedDate.minusMinutes(15)))
      return true;
    
    // default, not modified
    return false;
  }
  
  /**
   * Check if this group is enabled or not.
   */
  public boolean isEnabled() {
    boolean enabled = !this.isDeleted();
    if (parent != null) {
      enabled &= parent.isEnabled();
    }
    return enabled;
  }
  
  /**
   * Check if this group is disabled or or not.
   */
  public boolean isDisabled() {
    return !this.isEnabled();
  }
  
  /**
   * Groups must have a parent to be removed when we get rid of Levels.
   */
  @AssertTrue
  public boolean isValid() {
    return this.getParent() != null;
  }
}