package test;

public class t02 {
public static void main(String[] args) {
	Person p=new Person();
	p.setFirstName("mk");
	//p.setMiddleName("bk");
	p.setLastName("ck");
	Person p1=new Person();
	p1.setFirstName("mk");
	p1.setMiddleName("bk");
	p1.setLastName("ck");

	Person p2=new Person();
	p2.setFirstName("mk");
	p.setMiddleName("bk");
	//p2.setLastName("ck");
	Person[] pl= {p,p1,p2};
//	pl[0]=p;
//	pl[1]=p1;
//	pl[2]=p2;	
	System.out.println(pl);
	for (Person person : pl) {
		System.out.println(person.toString());
		person.setName(person);
	}
	for (Person person : pl) {
		System.out.println(person.toString());
	}
}
}
