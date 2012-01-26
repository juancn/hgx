package codng.hgx;

import java.io.Serializable;

public class Id 
		implements Serializable 
{
	public final long seqNo; 
	public final String hash;

	Id(long seqNo, String hash) {
		this.seqNo = seqNo;
		this.hash = hash;
	}

	@Override
	public int hashCode() {
		return (int)seqNo + 37*hash.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Id) {
			Id other = (Id) obj;
			return seqNo == other.seqNo && hash.equals(other.hash);
		}
		return false;
	}

	@Override
	public String toString() {
		return seqNo + ":" + hash;
	}

	static Id parse(String s) {
		final String[] parts = s.split(":");
		if(parts.length != 2) {
			throw new IllegalArgumentException("Cannot parse: " + s);
		}
		return new Id(Long.parseLong(parts[0]), parts[1]);
	}
}
