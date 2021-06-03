package test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;


public class Person {
public String firstName;
public String middleName;
public String lastName;
public String name;
public String getFirstName() {
	return firstName;
}
public void setFirstName(String firstName) {
	this.firstName = firstName;
}
public String getMiddleName() {
	return middleName;
}
public void setMiddleName(String middleName) {
	this.middleName = middleName;
}
public String getLastName() {
	return lastName;
}
public void setLastName(String lastName) {
	this.lastName = lastName;
}
public String getName() {
	return name;
}
public void setName(String name) {
	this.name = name;
}
public void setName(Person hcirRep) {
	List<String> names = Arrays.asList(hcirRep.firstName,hcirRep.middleName,hcirRep.lastName);
	 String fullName = names.stream().filter(name -> StringUtils.isNotBlank(name)).collect(Collectors.joining(" "));
	 this.name =fullName;
}
//public Person(String firstName, String middleName, String lastName, String name) {
//	super();
//	this.firstName = firstName;
//	this.middleName = middleName;
//	this.lastName = lastName;
//	this.name = name;
//}
@Override
public String toString() {
	return "Person [firstName=" + firstName + ", middleName=" + middleName + ", lastName=" + lastName + ", name=" + name
			+ "]";
}

}
