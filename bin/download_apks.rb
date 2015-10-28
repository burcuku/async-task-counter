require 'rubygems'
require 'mechanize'

agent = Mechanize.new

page = agent.get('https://f-droid.org/repository/browse/')
File.open('links.txt', 'w') do |file|  


agent.pluggable_parser.default = Mechanize::Download


pages = page.links_with(:href => %r{https:\/\/f-droid.org\/repository\/browse\/\?fdpage=}).each do|nextpageurl|
	nextpage = agent.get(nextpageurl.href)
	nextpage.links_with(:href => %r{https:\/\/f-droid.org\/repository\/browse\/\?fdid=}).each do|link|
	file.puts link.href

	downloadpage = agent.get(link.href)

	oy = downloadpage.link_with(:text => 'download apk')

	if(oy)
		length = oy.href.length 
		name = oy.href[25, length-1]
		agent.get(oy.href).save(name)
	end
	end
end
end  







