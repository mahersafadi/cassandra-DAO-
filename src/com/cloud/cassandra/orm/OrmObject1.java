package com.cloud.cassandra.orm;

import com.cloud.cassandra.annotation.*;

@KeySpace(name="test2")
@ColumnFamily(name="s1_consumption")
public class OrmObject1 {
	@Column(name="appmc", compositKey=true, type=Types.Varchar)
	private String appMac;
	
	@Column(name="home", partionKey=true, type=Types.BigInt)
	private Long homeId;
	
	@Column(name="ctime", sliceKey=true, type=Types.BigInt)
	private Long creationTime;
	
	@Column(name="left_cons", type=Types.Float)
	private Float leftConsumption;
	
	@Column(name="right_cons", type=Types.Float)
	private Float rightConsumption;
	
	@Column(name="left_data", type=Types.Text)
	private String leftData;
	
	@Column(name="right_data", type=Types.Text)
	private String rightData;
	public OrmObject1() {
		// TODO Auto-generated constructor stub
	}
	public OrmObject1(String appMac, Long homeId, Long creationTime,
			Float leftConsumption, Float rightConsumption, String leftData,
			String rightData) {
		super();
		this.appMac = appMac;
		this.homeId = homeId;
		this.creationTime = creationTime;
		this.leftConsumption = leftConsumption;
		this.rightConsumption = rightConsumption;
		this.leftData = leftData;
		this.rightData = rightData;
	}
	public String getAppMac() {
		return appMac;
	}
	public void setAppMac(String appMac) {
		this.appMac = appMac;
	}
	public Long getHomeId() {
		return homeId;
	}
	public void setHomeId(Long homeId) {
		this.homeId = homeId;
	}
	public Long getCreationTime() {
		return creationTime;
	}
	public void setCreationTime(Long creationTime) {
		this.creationTime = creationTime;
	}
	public Float getLeftConsumption() {
		return leftConsumption;
	}
	public void setLeftConsumption(Float leftConsumption) {
		this.leftConsumption = leftConsumption;
	}
	public Float getRightConsumption() {
		return rightConsumption;
	}
	public void setRightConsumption(Float rightConsumption) {
		this.rightConsumption = rightConsumption;
	}
	public String getLeftData() {
		return leftData;
	}
	public void setLeftData(String leftData) {
		this.leftData = leftData;
	}
	public String getRightData() {
		return rightData;
	}
	public void setRightData(String rightData) {
		this.rightData = rightData;
	}
}
