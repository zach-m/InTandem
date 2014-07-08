package tectonica.test.cindy.model;

import javax.xml.bind.annotation.XmlRootElement;

import tectonica.cindy.framework.Entity;

@XmlRootElement
public class Person implements Entity
{
	public String id;
	public long subId;
	public String name;
	public int age;
	public double height;

	public static Person create(String id, long subId, String name, int age, double height)
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
}
