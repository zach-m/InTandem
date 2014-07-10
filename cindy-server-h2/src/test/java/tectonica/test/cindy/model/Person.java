package tectonica.test.cindy.model;

import javax.xml.bind.annotation.XmlRootElement;

import tectonica.cindy.framework.Entity;
import tectonica.cindy.framework.PatchableEntity;

@XmlRootElement
public class Person implements PatchableEntity
{
	public String id;
	public long subId;
	public String name;
	public Integer age;
	public Double height;

	public static Person create(String id, long subId, String name, Integer age, Double height)
	{
		Person person = new Person();
		person.id = id;
		person.subId = subId;
		person.name = name;
		person.age = age;
		person.height = height;
		return person;
	}

	@Override
	public String toString()
	{
		return "Person [id=" + id + ", subId=" + subId + ", name=" + name + ", age=" + age + ", height=" + height + "]";
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public long getSubId()
	{
		return subId;
	}

	@Override
	public String getType()
	{
		return "person";
	}

	@Override
	public void patchWith(Entity partialEntity)
	{
		Person person = (Person) partialEntity;
		if (person.name != null)
			name = person.name;
		if (person.age != null)
			age = person.age;
		if (person.height != null)
			height = person.height;
	}
}