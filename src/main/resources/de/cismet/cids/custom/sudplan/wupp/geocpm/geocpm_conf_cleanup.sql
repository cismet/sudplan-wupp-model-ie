-----------------------------------------------------------------------------
-- Deletes all records belong to the GeoCPM configuration with the given id
--
-- PARAM: 
--          (integer) geocpm configuration id
-- RETURNS: VOID
--
-- AUTHOR: Benjamin Friedrich (benjamin.friedrich@cismet.de)
-- DATE:   03.09.2012
-----------------------------------------------------------------------------


CREATE OR REPLACE FUNCTION geocpm_clean_up(integer) RETURNS void AS $$
DECLARE
    geocpm_conf_id ALIAS FOR $1;
BEGIN


--- DELTA CLEAN-UP
  

  delete from delta_configuration_delta_breaking_edge 
  where delta_configuration_reference in ( select id 
					   from delta_configuration 
					   where original_object = geocpm_conf_id);
  
  delete from delta_surface 
  where delta_configuration in ( select id 
				 from delta_configuration 
				 where original_object = geocpm_conf_id);

  delete from delta_breaking_edge
  where original_object in (select id 
                            from geocpm_breaking_edge 
                            where geocpm_configuration_id = geocpm_conf_id);

  delete from delta_configuration 
  where original_object = geocpm_conf_id;
    
----------


  delete from geocpm_aus_info where geocpm_configuration_id = geocpm_conf_id;
  delete from geocpm_aus_max  where geocpm_configuration_id = geocpm_conf_id;


  delete from geocpm_jt_breaking_edge_triangle where geocpm_breaking_edge_id in 
								(select id 
								  from  geocpm_breaking_edge 
								  where geocpm_configuration_id = geocpm_conf_id);
								  
  delete from geocpm_breaking_edge where geocpm_configuration_id = geocpm_conf_id;



  delete from geocpm_jt_manhole_triangle where geocpm_triangle_id in
								(select id 
								 from   geocpm_triangle
								 where  geocpm_configuration_id = geocpm_conf_id);

  delete from geocpm_manhole where geocpm_configuration_id = geocpm_conf_id;
   
  delete from geocpm_source_drain where geocpm_configuration_id = geocpm_conf_id;
  
  delete from geocpm_point    where geocpm_configuration_id = geocpm_conf_id;

  delete from geocpm_triangle where geocpm_configuration_id = geocpm_conf_id;

  delete from geocpm_curve_value where geocpm_curve_id in (select id from geocpm_curve where geocpm_configuration_id = geocpm_conf_id);
  
  delete from geocpm_curve where geocpm_configuration_id = geocpm_conf_id;


  delete from geocpm_configuration where id = geocpm_conf_id;

--- GEOM CLEAN UP
  delete from geom where geom.id = (select geom from geocpm_configuration where id = geocpm_conf_id LIMIT 1);
  
  delete from geom where geom.id in (select geom from geocpm_breaking_edge where geocpm_configuration_id = geocpm_conf_id);
  
  delete from geom where geom.id in (
					select  geom 
					from    delta_surface 
					where   delta_configuration = 
							(
								select id 
								from delta_configuration 
								where original_object = geocpm_conf_id
							)
				    );
  
END;
$$ LANGUAGE plpgsql;