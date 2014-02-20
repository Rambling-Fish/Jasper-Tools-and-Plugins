package com.coralcea.jasper.tools.dta.diagrams;

import java.util.List;

import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;

import com.coralcea.jasper.tools.MultiValueMap;

public class DTABendpointConnectionRouter extends BendpointConnectionRouter {

	private int separation = 40;
	private MultiValueMap<Rectangle, Connection> connections = new MultiValueMap<Rectangle, Connection>();

	public DTABendpointConnectionRouter() {
	}

	public DTABendpointConnectionRouter(int separation) {
		this.separation = separation;
	}
	
	public int getSeparation() {
		return this.separation;  
	}

	private boolean isSelfConnection(Connection conn) {
		ConnectionAnchor source = conn.getSourceAnchor();
		ConnectionAnchor target = conn.getTargetAnchor();
		return source != null && target!= null && (source.getOwner() == target.getOwner());
	}
	
	@Override
	public void route(Connection conn) {
		super.route(conn);
		if (isSelfConnection(conn))
			routeSelfConnection(conn);
	}
	
	public void routeSelfConnection(Connection conn) {
		Rectangle r = conn.getSourceAnchor().getOwner().getBounds();
		int index;
		
		List<Connection> connectionList = connections.get(r);
		if (connectionList == null) 
			connections.put(r, conn);
		connectionList = connections.get(r);
		if (connectionList.contains(conn)) {
			index = connectionList.indexOf(conn) + 1;	
		} 
		else {
			index = connectionList.size() + 1;
			connections.put(r, conn);
		}

		int dw = 20;
		int dh = 20;
		
		PointList points = conn.getPoints();
		points.removeAllPoints();
		points.addPoint(new Point(r.x+r.width, r.y+dw));
		points.addPoint(new Point(r.x+r.width+dw, r.y+dw));
		points.addPoint(new Point(r.x+r.width+dw, r.y-dh*index));
		points.addPoint(new Point(r.x+r.width-dw, r.y-dh*index));
		points.addPoint(new Point(r.x+r.width-dw, r.y));
	    conn.setPoints( points );
	}

	@Override
	public void invalidate(Connection conn) {
		super.invalidate(conn);
		if (isSelfConnection(conn)) {
			Rectangle r = conn.getSourceAnchor().getOwner().getBounds();
			List<Connection> connectionList = connections.get(r);
			int affected = connections.remove(r, conn); 
			if (affected != -1) {
				for (int i = affected; i < connectionList.size(); i++)
					((Connection)connectionList.get(i)).revalidate();
			} else
				connections.removeValue(conn);
		}
	}

	@Override
	public void remove(Connection conn) {
		super.remove(conn);
		if (isSelfConnection(conn)) {
			Rectangle r = conn.getSourceAnchor().getOwner().getBounds();
			List<Connection> connectionList = connections.get(r);
			if (connectionList != null) {
				int index = connections.remove(r, conn);		
				for (int i = index + 1; i < connectionList.size(); i++)
					((Connection)connectionList.get(i)).revalidate();
			}		
		}
	}
}
