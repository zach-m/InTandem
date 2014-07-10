package tectonica.test.intandem.model;

import javax.xml.bind.annotation.XmlRootElement;

import tectonica.intandem.framework.Entity;
import tectonica.intandem.framework.PatchableEntity;

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
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Person other = (Person) obj;
		if (age == null)
		{
			if (other.age != null)
				return false;
		}
		else if (!age.equals(other.age))
			return false;
		if (height == null)
		{
			if (other.height != null)
				return false;
		}
		else if (!height.equals(other.height))
			return false;
		if (id == null)
		{
			if (other.id != null)
				return false;
		}
		else if (!id.equals(other.id))
			return false;
		if (name == null)
		{
			if (other.name != null)
				return false;
		}
		else if (!name.equals(other.name))
			return false;
		if (subId != other.subId)
			return false;
		return true;
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
